package com.example.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ViraApp
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ViraApp ?: return Result.failure()
        val syncManager = app.container.syncManager

        return try {
            val allSuccess = syncManager.syncAll()
            if (allSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "vira_sync_worker"
        const val WORK_NAME_PERIODIC = "vira_periodic_sync_worker"

        fun scheduleSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                android.util.Log.w("SyncWorker", "scheduleSync skipped: ${e.message}")
            }
        }

        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 15) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                android.util.Log.w("SyncWorker", "schedulePeriodicSync skipped: ${e.message}")
            }
        }
    }
}
