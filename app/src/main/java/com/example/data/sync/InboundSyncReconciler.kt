package com.example.data.sync

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.GoalEntity
import com.example.data.local.LocalUserProfileEntity
import com.example.data.local.RedemptionEntryEntity
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant

/**
 * Reconciles remote authoritative server changes into local Room storage.
 * Implements strict outbox protection (never overwrites pending local un-synced edits)
 * and applies atomic transactional updates.
 */
open class InboundSyncReconciler(
    private val database: AppDatabase,
    private val cursorManager: SyncCursorManager? = null
) {

    companion object {
        private const val TAG = "ViraSync"
    }

    open suspend fun reconcile(response: RemoteSyncPullResponse, currentCursor: Long = 0L) {
        database.withTransaction {
            android.util.Log.d(
                TAG,
                "Reconcile: starting transaction. currentCursor=$currentCursor, remoteNewCursor=${response.newCursor}"
            )
            reconcileSpots(response.spots)
            reconcileCollections(response.collections)
            reconcileRedemptions(response.redemptions)
            reconcileGoals(response.goals)
            response.profile?.let { reconcileProfile(it) }

            if (response.newCursor > currentCursor) {
                cursorManager?.setCursor(response.newCursor)
                android.util.Log.i(TAG, "Cursor advanced: $currentCursor -> ${response.newCursor}")
            }
        }
    }

    private suspend fun reconcileCollections(remoteList: List<RemoteCollectionDto>) {
        if (remoteList.isEmpty()) return
        val localList = database.collectionDao().getAllCollections().firstOrNull() ?: emptyList()
        for (remote in remoteList) {
            if (isPendingOutbox(remote.remoteId)) {
                android.util.Log.d(TAG, "Reconcile skip [Outbox Protection]: Collection remoteId=${remote.remoteId}")
                continue
            }
            val local = localList.find { it.remoteId == remote.remoteId }
            val serverMillis = parseIsoToMillis(remote.serverUpdatedAt)
            val version = remote.serverVersion ?: 1L

            if (remote.deletedAt != null) {
                local?.let { database.collectionDao().deleteCollectionById(it.id) }
            } else {
                val entity = local?.copy(
                    containerCount = remote.containerCount,
                    timestamp = remote.timestamp,
                    estimatedValueCents = remote.estimatedValueCents,
                    note = remote.note,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    updatedAt = serverMillis
                ) ?: CollectionEntryEntity(
                    remoteId = remote.remoteId,
                    containerCount = remote.containerCount,
                    timestamp = remote.timestamp,
                    estimatedValueCents = remote.estimatedValueCents,
                    note = remote.note,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    createdAt = remote.timestamp,
                    updatedAt = serverMillis
                )
                database.collectionDao().insertCollection(entity)
            }
        }
    }

    private suspend fun reconcileSpots(remoteList: List<RemoteSpotDto>) {
        if (remoteList.isEmpty()) return
        val localList = database.spotDao().getAllSpots().firstOrNull() ?: emptyList()
        for (remote in remoteList) {
            if (isPendingOutbox(remote.remoteId)) {
                android.util.Log.d(TAG, "Reconcile skip [Outbox Protection]: Spot remoteId=${remote.remoteId}")
                continue
            }
            val local = localList.find { it.remoteId == remote.remoteId }
            val serverMillis = parseIsoToMillis(remote.serverUpdatedAt)
            val version = remote.serverVersion ?: 1L

            if (remote.deletedAt != null) {
                android.util.Log.i(TAG, "Reconcile tombstone: Spot remoteId=${remote.remoteId}")
                local?.let { database.spotDao().deleteSpotById(it.id) }
            } else {
                val entity = local?.copy(
                    name = remote.name,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    address = remote.address,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    updatedAt = serverMillis
                ) ?: CollectionSpotEntity(
                    remoteId = remote.remoteId,
                    name = remote.name,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    address = remote.address,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    createdAt = remote.clientCreatedAt,
                    updatedAt = serverMillis
                )
                database.spotDao().insertSpot(entity)
            }
        }
    }

    private suspend fun reconcileRedemptions(remoteList: List<RemoteRedemptionDto>) {
        if (remoteList.isEmpty()) return
        val localList = database.redemptionDao().getAllRedemptions().firstOrNull() ?: emptyList()
        for (remote in remoteList) {
            if (isPendingOutbox(remote.remoteId)) {
                android.util.Log.d(TAG, "Reconcile skip [Outbox Protection]: Redemption remoteId=${remote.remoteId}")
                continue
            }
            val local = localList.find { it.remoteId == remote.remoteId }
            val serverMillis = parseIsoToMillis(remote.serverUpdatedAt)
            val version = remote.serverVersion ?: 1L

            if (remote.deletedAt != null) {
                android.util.Log.i(TAG, "Reconcile tombstone: Redemption remoteId=${remote.remoteId}")
                local?.let { database.redemptionDao().deleteRedemptionById(it.id) }
            } else {
                val entity = local?.copy(
                    presentedContainers = remote.presentedContainers,
                    acceptedContainers = remote.acceptedContainers,
                    rejectedContainers = remote.rejectedContainers,
                    actualRecoveredCents = remote.actualRecoveredCents,
                    timestamp = remote.timestamp,
                    note = remote.note,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    updatedAt = serverMillis
                ) ?: RedemptionEntryEntity(
                    remoteId = remote.remoteId,
                    presentedContainers = remote.presentedContainers,
                    acceptedContainers = remote.acceptedContainers,
                    rejectedContainers = remote.rejectedContainers,
                    actualRecoveredCents = remote.actualRecoveredCents,
                    timestamp = remote.timestamp,
                    note = remote.note,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    createdAt = remote.timestamp,
                    updatedAt = serverMillis
                )
                database.redemptionDao().insertRedemption(entity)
            }
        }
    }

    private suspend fun reconcileGoals(remoteList: List<RemoteGoalDto>) {
        if (remoteList.isEmpty()) return
        val localList = database.goalDao().getAllGoals().firstOrNull() ?: emptyList()
        for (remote in remoteList) {
            if (isPendingOutbox(remote.remoteId)) {
                android.util.Log.d(TAG, "Reconcile skip [Outbox Protection]: Goal remoteId=${remote.remoteId}")
                continue
            }
            val local = localList.find { it.remoteId == remote.remoteId }
            val serverMillis = parseIsoToMillis(remote.serverUpdatedAt)
            val version = remote.serverVersion ?: 1L

            if (remote.deletedAt != null) {
                android.util.Log.i(TAG, "Reconcile tombstone: Goal remoteId=${remote.remoteId}")
                local?.let { database.goalDao().deleteGoalById(it.id) }
            } else {
                val entity = local?.copy(
                    type = remote.type,
                    targetValue = remote.targetValue,
                    period = remote.period,
                    isActive = remote.isActive,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    updatedAt = serverMillis
                ) ?: GoalEntity(
                    remoteId = remote.remoteId,
                    type = remote.type,
                    targetValue = remote.targetValue,
                    period = remote.period,
                    isActive = remote.isActive,
                    syncState = SyncState.SYNCED.name,
                    serverUpdatedAt = serverMillis,
                    remoteVersion = version,
                    createdAt = remote.clientUpdatedAt,
                    updatedAt = serverMillis
                )
                database.goalDao().insertGoal(entity)
            }
        }
    }

    private suspend fun reconcileProfile(remote: RemoteProfileDto) {
        if (isPendingOutbox(remote.remoteId)) {
            android.util.Log.d(TAG, "Reconcile skip [Outbox Protection]: UserProfile remoteId=${remote.remoteId}")
            return
        }
        val local = database.userDao().getUserProfile().firstOrNull()
        val serverMillis = parseIsoToMillis(remote.serverUpdatedAt)
        val version = remote.serverVersion ?: 1L

        if (remote.deletedAt != null) {
            android.util.Log.i(TAG, "Reconcile tombstone: UserProfile remoteId=${remote.remoteId}")
            database.userDao().clearUserProfile()
        } else {
            val entity = local?.copy(
                name = remote.name,
                themePreference = remote.themePreference,
                syncState = SyncState.SYNCED.name,
                serverUpdatedAt = serverMillis,
                remoteVersion = version,
                updatedAt = serverMillis
            ) ?: LocalUserProfileEntity(
                remoteId = remote.remoteId,
                name = remote.name,
                memberSince = remote.clientUpdatedAt,
                monthlyGoalId = null,
                themePreference = remote.themePreference,
                syncState = SyncState.SYNCED.name,
                serverUpdatedAt = serverMillis,
                remoteVersion = version,
                updatedAt = serverMillis
            )
            database.userDao().insertUserProfile(entity)
        }
    }

    private suspend fun isPendingOutbox(remoteId: String): Boolean {
        return database.syncOutboxDao().getPendingByRemoteId(remoteId) != null
    }

    private fun parseIsoToMillis(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
