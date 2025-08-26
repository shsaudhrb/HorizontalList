package com.ntg.lmd.network.queue

import com.ntg.lmd.network.queue.storage.QueuedRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

fun QueuedRequest.toRequestBodyOrNull(): RequestBody? {
    val text = bodyText ?: return null
    val ct = (contentType?.takeIf { it.isNotBlank() } ?: "application/json").toMediaTypeOrNull()
    return text.toRequestBody(ct)
}
