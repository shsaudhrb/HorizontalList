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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit

private const val TAG_REQ = "RequestRepo"

class RequestRepository(
    private val dao: RequestDao,
    private val retrofit: Retrofit,
    private val network: NetworkMonitor,
) {
    private val channel = Channel<Long>(capacity = 200, onBufferOverflow = BufferOverflow.SUSPEND)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val drainMutex = Mutex()

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

    suspend fun enqueue(req: QueuedRequest): Long {
        val id = dao.insert(req)
        Log.d(TAG_REQ, "enqueue id=$id method=${req.method} url=${req.url}")
        channel.send(id)
        return id
    }

    private suspend fun drainOne(id: Long) {
        Log.d(TAG_REQ, "drainOne start id=$id")
        val item = dao.all().firstOrNull { it.id == id } ?: run {
            Log.d(TAG_REQ, "drainOne id=$id not found"); return
        }
        if (!network.isOnline.value) {
            Log.d(TAG_REQ, "drainOne id=$id skipped (offline)"); return
        }

        val result = runCatching { execute(item) }
        result.onSuccess {
            dao.delete(item.id)
            Log.d(TAG_REQ, "drainOne id=${item.id} -> SUCCESS (deleted)")
        }.onFailure { e ->
            dao.bumpAttempts(item.id)
            Log.w(TAG_REQ, "drainOne id=${item.id} -> FAIL (attempts++), err=${e.message}")
        }
    }


    suspend fun drainAll() =
        drainMutex.withLock {
            val all = dao.all()
            Log.d(TAG_REQ, "drainAll size=${all.size}")
            for (item in all) drainOne(item.id)
            Log.d(TAG_REQ, "drainAll done")
        }

    private suspend fun execute(item: QueuedRequest) {
        val client = retrofit.callFactory() as OkHttpClient
        val req = okhttp3.Request.Builder()
            .url(item.url)
            .method(item.method.uppercase(), null)
            .build()

        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        resp.use {
            check(it.isSuccessful) { "HTTP ${it.code} for ${item.method} ${item.url}" }
        }
    }


    fun syncOutboxOnce(): Flow<List<QueuedRequest>> =
        networkBoundResource(
            query = { dao.all() },
            shouldFetch = { true },
            fetch = {
                drainAll()
            },
            saveFetchResult = { _: Unit -> },
        )
}
