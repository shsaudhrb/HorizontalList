package com.ntg.lmd.network.queue.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RequestDao {
    @Insert
    suspend fun insert(req: QueuedRequest): Long

    @Query("SELECT * FROM request ORDER BY id ASC")
    suspend fun all(): List<QueuedRequest>

    @Query("DELETE FROM request WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE request SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpAttempts(id: Long)
}
