package com.ntg.lmd.network.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

inline fun <ResultT, RequestT> networkBoundResource(
    crossinline query: suspend () -> ResultT,
    crossinline fetch: suspend () -> RequestT,
    crossinline saveFetchResult: suspend (RequestT) -> Unit,
    crossinline shouldFetch: (ResultT?) -> Boolean = { true },
): Flow<ResultT> =
    flow {
        val local = runCatching { query() }.getOrNull()
        if (local != null) emit(local)
        if (shouldFetch(local)) {
            runCatching {
                val remote = fetch()
                saveFetchResult(remote)
                emit(query())
            }.onFailure { if (local == null) throw it }
        }
    }.flowOn(Dispatchers.IO)
