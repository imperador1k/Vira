package com.example.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ViraApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ViraApp ?: return Result.failure()
        val container = app.container

        val userId = container.authRepository.getCurrentUserId()
        if (userId == null) {
            // Unauthenticated: cannot perform cloud backup
            return Result.success()
        }

        val prefs = container.backupPreferencesRepository.preferences.first()
        if (!prefs.isAutoBackupEnabled) {
            return Result.success()
        }

        return try {
            val payload = container.backupManager.createSnapshotPayload()
            val backupType = if (prefs.frequency == BackupFrequency.WEEKLY) "WEEKLY" else "DAILY"
            val uploadResult = container.backupManager.uploadSnapshot(payload, backupType)

            if (uploadResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ViraAutoBackupWorker"

        fun scheduleOrCancel(context: Context, preferences: BackupPreferences) {
            val workManager = WorkManager.getInstance(context)
            if (!preferences.isAutoBackupEnabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val intervalDays = if (preferences.frequency == BackupFrequency.WEEKLY) 7L else 1L
            val networkType = if (preferences.networkConstraint == BackupNetworkConstraint.WIFI_ONLY) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
