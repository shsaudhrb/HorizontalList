package com.ntg.lmd.network.queue

import android.util.Log
import com.ntg.lmd.network.connectivity.NetworkMonitor
import com.ntg.lmd.network.core.networkBoundResource
import com.ntg.lmd.network.queue.storage.QueuedRequest
import com.ntg.lmd.network.queue.storage.RequestDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.RequestBody
import retrofit2.Retrofit
import java.io.IOException

private const val TAG_REQ = "RequestRepo"
private const val REQUEST_TIMEOUT_MS = 30_000L

class RequestRepository(
    private val dao: RequestDao,
    private val retrofit: Retrofit,
    private val network: NetworkMonitor,
) {
    private val channel = Channel<Long>(capacity = 200, onBufferOverflow = BufferOverflow.SUSPEND)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val drainMutex = Mutex()

    // (1) Use QueueApiService via Retrofit (from comment #1)
    private val queueApiService: QueueApiService by lazy {
        retrofit.create(QueueApiService::class.java)
    }

    init {
        scope.launch {
            for (id in channel) drainOne(id)
        }
        scope.launch {
            network.isOnline.collect { online ->
                if (online) drainAll()
            }
        }
    }

    // (2) Return NetworkResult instead of throwing (comment #4)
    suspend fun enqueue(req: QueuedRequest): NetworkResult<Long> =
        try {
            val id = dao.insert(req)
            Log.d(TAG_REQ, "enqueue id=$id method=${req.method} url=${req.url}")
            channel.send(id)
            NetworkResult.Success(id)
        } catch (e: IOException) {
            Log.e(TAG_REQ, "Failed to enqueue request", e)
            NetworkResult.Error(NetworkError.fromException(e))
        }

    // (3) drainOne uses when(result) (comment #3)
    private suspend fun drainOne(id: Long) {
        Log.d(TAG_REQ, "drainOne start id=$id")

        val item =
            dao.all().firstOrNull { it.id == id } ?: run {
                Log.d(TAG_REQ, "drainOne id=$id not found")
                return
            }

        if (!network.isOnline.value) {
            Log.d(TAG_REQ, "drainOne id=$id skipped (offline)")
            return
        }

        val result = executeAsync(item)
        when (result) {
            is NetworkResult.Success -> {
                dao.delete(item.id)
                Log.d(TAG_REQ, "drainOne id=${item.id} -> SUCCESS (deleted)")
            }

            is NetworkResult.Error -> {
                dao.bumpAttempts(item.id)
                Log.w(
                    TAG_REQ,
                    "drainOne id=${item.id} -> FAIL (attempts++), error=${result.error.message}",
                )
            }

            is NetworkResult.Loading -> {
                Log.w(TAG_REQ, "drainOne id=${item.id} -> Unexpected loading state")
            }
        }
    }

    suspend fun drainAll() =
        drainMutex.withLock {
            val all = dao.all()
            Log.d(TAG_REQ, "drainAll size=${all.size}")
            for (item in all) drainOne(item.id)
            Log.d(TAG_REQ, "drainAll done")
        }

    private suspend fun executeAsync(item: QueuedRequest): NetworkResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(REQUEST_TIMEOUT_MS) {
                    val response =
                        when (item.method.uppercase()) {
                            "GET" -> queueApiService.get(item.url)
                            "POST" -> queueApiService.post(item.url, item.bodyText as RequestBody?)
                            "PUT" -> queueApiService.put(item.url, item.bodyText as RequestBody?)
                            "DELETE" -> queueApiService.delete(item.url)
                            "PATCH" -> queueApiService.patch(item.url, item.bodyText as RequestBody?)
                            else -> throw IllegalArgumentException("Unsupported HTTP method: ${item.method}")
                        }
                    if (response.isSuccessful) {
                        NetworkResult.Success(Unit)
                    } else {
                        NetworkResult.Error(
                            NetworkError.fromHttpCode(
                                response.code(),
                                "HTTP ${response.code()} for ${item.method} ${item.url}",
                            ),
                        )
                    }
                }
            } catch (e: IOException) {
                when (e) {
                    is IllegalArgumentException ->
                        NetworkResult.Error(
                            NetworkError.BadRequest(
                                e.message ?: "Invalid request",
                            ),
                        )

                    else -> NetworkResult.Error(NetworkError.fromException(e))
                }
            }
        }

    fun syncOutboxOnce() =
        networkBoundResource(
            query = { dao.all() },
            shouldFetch = { true },
            fetch = { drainAll() },
            saveFetchResult = { _: Unit -> },
        )
}
