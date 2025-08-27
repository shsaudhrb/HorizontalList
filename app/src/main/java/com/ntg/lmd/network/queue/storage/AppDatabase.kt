package com.ntg.lmd.network.queue.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [QueuedRequest::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun requestDao(): RequestDao
}
