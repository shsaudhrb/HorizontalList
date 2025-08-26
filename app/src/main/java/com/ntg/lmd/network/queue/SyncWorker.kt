package com.ntg.lmd.network.queue

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.JsonParseException

class SyncWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        try {
            val repo = ServiceLocator.requestRepository(applicationContext)
            repo.drainAll()
            Result.success()
        } catch (e: JsonParseException) {
            Result.retry()
        }

    companion object {
        fun enqueue(ctx: Context) {
            val req =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()
            WorkManager.getInstance(ctx).enqueue(req)
        }
    }
}
