package com.ntg.lmd.network.queue.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "request")
data class QueuedRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val url: String,
    val headersJson: String? = null,
    val bodyText: String? = null,
    val contentType: String? = null,
    val attempts: Int = 0,
)
