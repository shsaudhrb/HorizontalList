package com.ntg.lmd.network.queue

sealed class NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>()

    data class Success<T>(
        val data: T,
    ) : NetworkResult<T>()

    data class Error(
        val error: NetworkError,
    ) : NetworkResult<Nothing>()
}
