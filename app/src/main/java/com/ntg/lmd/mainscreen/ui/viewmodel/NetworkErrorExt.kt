package com.ntg.lmd.mainscreen.ui.viewmodel

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val HTTP_BAD_REQUEST = HttpURLConnection.HTTP_BAD_REQUEST // 400
private const val HTTP_UNAUTHORIZED = HttpURLConnection.HTTP_UNAUTHORIZED // 401
private const val HTTP_FORBIDDEN = HttpURLConnection.HTTP_FORBIDDEN // 403
private const val HTTP_NOT_FOUND = HttpURLConnection.HTTP_NOT_FOUND // 404
private const val HTTP_SERVER_ERROR_MIN = HttpURLConnection.HTTP_INTERNAL_ERROR // 500
private const val HTTP_SERVER_ERROR_MAX = 599

fun Throwable.toUserMessage(): String {
    if (this is CancellationException) throw this
    return when (this) {
        is HttpException -> httpStatusMessage(this.code(), this.message())
        is UnknownHostException, is java.net.ConnectException ->
            "No internet connection."

        is SocketTimeoutException ->
            "Request timed out. Please try again."

        is javax.net.ssl.SSLHandshakeException ->
            "Secure connection failed."

        else -> "Unexpected error. Please try again."
    }
}

private fun httpStatusMessage(
    code: Int,
    raw: String?,
): String =
    when (code) {
        HTTP_BAD_REQUEST -> "Invalid request. Please try again."
        HTTP_UNAUTHORIZED -> "Session expired. Please sign in again."
        HTTP_FORBIDDEN -> "You don’t have permission to perform this action."
        HTTP_NOT_FOUND -> "Order not found."
        in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> "Server error. Please try later."
        else -> "HTTP $code: ${raw ?: "Unexpected error"}"
    }
