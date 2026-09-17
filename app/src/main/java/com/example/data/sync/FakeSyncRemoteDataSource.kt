package com.example.data.sync

import com.example.data.local.OutboxEntityType
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory fake remote data source for testing and account-less local execution.
 * Faithfully mirrors PostgreSQL global monotonic sequence and BEFORE INSERT OR UPDATE triggers.
 */
class FakeSyncRemoteDataSource(
    var shouldFailWithNetworkError: Boolean = false,
    var shouldFailWithConflict: Boolean = false,
    var failingRemoteIds: Set<String> = emptySet(),
    var currentUserId: String? = "test-default-user"
) : SyncRemoteDataSource {

    val versionCounter = AtomicLong(0L)
    val pushedCollections = mutableListOf<RemoteCollectionDto>()
    val pushedSpots = mutableListOf<RemoteSpotDto>()
    val pushedRedemptions = mutableListOf<RemoteRedemptionDto>()
    val pushedGoals = mutableListOf<RemoteGoalDto>()
    val pushedProfiles = mutableListOf<RemoteProfileDto>()
    val pushedFavorites = mutableListOf<RemoteFavoriteDto>()
    val deletedEntities = mutableListOf<Pair<String, String>>() // (type, remoteId)
    val entityOwners = mutableMapOf<String, String>() // remoteId -> userId

    var simulatedPullResponse: RemoteSyncPullResponse? = null

    fun setPullResponse(response: RemoteSyncPullResponse) {
        this.simulatedPullResponse = response
    }

    override suspend fun pushCollection(entry: RemoteCollectionDto): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[entry.remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: user cannot mutate another user's collection", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (failingRemoteIds.contains(entry.remoteId)) return RemoteSyncResult.NetworkError("Simulated network timeout for ${entry.remoteId}")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()
        val stored = entry.copy(
            serverVersion = newVersion,
            serverUpdatedAt = nowIso
        )
        entityOwners[entry.remoteId] = user
        pushedCollections.removeAll { it.remoteId == entry.remoteId }
        pushedCollections.add(stored)
        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pushRedemption(entry: RemoteRedemptionDto): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[entry.remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: user cannot mutate another user's redemption", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()
        val stored = entry.copy(
            serverVersion = newVersion,
            serverUpdatedAt = nowIso
        )
        entityOwners[entry.remoteId] = user
        pushedRedemptions.removeAll { it.remoteId == entry.remoteId }
        pushedRedemptions.add(stored)
        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pushSpot(spot: RemoteSpotDto): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[spot.remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: user cannot mutate another user's spot", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()
        val stored = spot.copy(
            serverVersion = newVersion,
            serverUpdatedAt = nowIso
        )
        entityOwners[spot.remoteId] = user
        pushedSpots.removeAll { it.remoteId == spot.remoteId }
        pushedSpots.add(stored)
        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pushGoal(goal: RemoteGoalDto): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[goal.remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: user cannot mutate another user's goal", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()
        val stored = goal.copy(
            serverVersion = newVersion,
            serverUpdatedAt = nowIso
        )
        entityOwners[goal.remoteId] = user
        pushedGoals.removeAll { it.remoteId == goal.remoteId }
        pushedGoals.add(stored)
        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pushProfile(profile: RemoteProfileDto): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[profile.remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: user cannot mutate another user's profile", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()
        val stored = profile.copy(
            serverVersion = newVersion,
            serverUpdatedAt = nowIso
        )
        entityOwners[profile.remoteId] = user
        pushedProfiles.removeAll { it.remoteId == profile.remoteId }
        pushedProfiles.add(stored)
        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pushFavorite(favorite: RemoteFavoriteDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedFavorites.removeAll { it.remoteId == favorite.remoteId }
        pushedFavorites.add(favorite)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun deleteEntity(entityType: String, remoteId: String): RemoteSyncResult {
        val user = currentUserId ?: return RemoteSyncResult.NetworkError("Unauthorized: authentication required", canRetry = false)
        val owner = entityOwners[remoteId]
        if (owner != null && owner != user) {
            return RemoteSyncResult.NetworkError("RLS Violation: cannot delete entity belonging to another user", canRetry = false)
        }
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        deletedEntities.add(entityType to remoteId)

        val newVersion = versionCounter.incrementAndGet()
        val nowIso = Instant.now().toString()

        // Create tombstone in memory so pullChanges serves the deletion
        when (entityType) {
            OutboxEntityType.COLLECTION_ENTRY.name -> {
                val existing = pushedCollections.find { it.remoteId == remoteId }
                if (existing != null) {
                    pushedCollections.removeAll { it.remoteId == remoteId }
                    pushedCollections.add(existing.copy(deletedAt = nowIso, serverVersion = newVersion))
                }
            }
            OutboxEntityType.COLLECTION_SPOT.name -> {
                val existing = pushedSpots.find { it.remoteId == remoteId }
                if (existing != null) {
                    pushedSpots.removeAll { it.remoteId == remoteId }
                    pushedSpots.add(existing.copy(deletedAt = nowIso, serverVersion = newVersion))
                }
            }
            OutboxEntityType.REDEMPTION_ENTRY.name -> {
                val existing = pushedRedemptions.find { it.remoteId == remoteId }
                if (existing != null) {
                    pushedRedemptions.removeAll { it.remoteId == remoteId }
                    pushedRedemptions.add(existing.copy(deletedAt = nowIso, serverVersion = newVersion))
                }
            }
            OutboxEntityType.GOAL.name -> {
                val existing = pushedGoals.find { it.remoteId == remoteId }
                if (existing != null) {
                    pushedGoals.removeAll { it.remoteId == remoteId }
                    pushedGoals.add(existing.copy(deletedAt = nowIso, serverVersion = newVersion))
                }
            }
            OutboxEntityType.USER_PROFILE.name -> {
                val existing = pushedProfiles.find { it.remoteId == remoteId }
                if (existing != null) {
                    pushedProfiles.removeAll { it.remoteId == remoteId }
                    pushedProfiles.add(existing.copy(deletedAt = nowIso, serverVersion = newVersion))
                }
            }
        }

        return RemoteSyncResult.Success(System.currentTimeMillis(), newVersion)
    }

    override suspend fun pullChanges(sinceCursor: Long): RemoteSyncPullResponse {
        if (shouldFailWithNetworkError) throw java.io.IOException("Simulated network timeout during pull")
        val user = currentUserId ?: throw IllegalStateException("Unauthorized: authentication required")
        val simulated = simulatedPullResponse
        if (simulated != null) return simulated

        val collections = pushedCollections.filter { (it.serverVersion ?: 0L) > sinceCursor && entityOwners[it.remoteId] == user }
        val spots = pushedSpots.filter { (it.serverVersion ?: 0L) > sinceCursor && entityOwners[it.remoteId] == user }
        val redemptions = pushedRedemptions.filter { (it.serverVersion ?: 0L) > sinceCursor && entityOwners[it.remoteId] == user }
        val goals = pushedGoals.filter { (it.serverVersion ?: 0L) > sinceCursor && entityOwners[it.remoteId] == user }
        val profile = pushedProfiles.find { (it.serverVersion ?: 0L) > sinceCursor && entityOwners[it.remoteId] == user }

        val maxCol = collections.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
        val maxSpot = spots.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
        val maxRed = redemptions.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
        val maxGoal = goals.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
        val maxProf = profile?.serverVersion ?: sinceCursor

        val nextCursor = maxOf(sinceCursor, maxCol, maxSpot, maxRed, maxGoal, maxProf)

        return RemoteSyncPullResponse(
            collections = collections,
            spots = spots,
            redemptions = redemptions,
            goals = goals,
            profile = profile,
            newCursor = nextCursor,
            hasMore = false
        )
    }
}
