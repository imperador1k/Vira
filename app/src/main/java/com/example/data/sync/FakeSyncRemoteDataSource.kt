package com.example.data.sync

import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory fake remote data source for testing and account-less local execution.
 */
class FakeSyncRemoteDataSource(
    var shouldFailWithNetworkError: Boolean = false,
    var shouldFailWithConflict: Boolean = false
) : SyncRemoteDataSource {

    private val versionCounter = AtomicLong(1L)
    val pushedCollections = mutableListOf<RemoteCollectionDto>()
    val pushedSpots = mutableListOf<RemoteSpotDto>()
    val pushedRedemptions = mutableListOf<RemoteRedemptionDto>()
    val pushedGoals = mutableListOf<RemoteGoalDto>()
    val pushedProfiles = mutableListOf<RemoteProfileDto>()
    val pushedFavorites = mutableListOf<RemoteFavoriteDto>()
    val deletedEntities = mutableListOf<Pair<String, String>>() // (type, remoteId)

    override suspend fun pushCollection(entry: RemoteCollectionDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        if (shouldFailWithConflict) return RemoteSyncResult.Conflict(versionCounter.get(), System.currentTimeMillis(), "Simulated conflict")
        pushedCollections.removeAll { it.remoteId == entry.remoteId }
        pushedCollections.add(entry)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun pushRedemption(entry: RemoteRedemptionDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedRedemptions.removeAll { it.remoteId == entry.remoteId }
        pushedRedemptions.add(entry)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun pushSpot(spot: RemoteSpotDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedSpots.removeAll { it.remoteId == spot.remoteId }
        pushedSpots.add(spot)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun pushGoal(goal: RemoteGoalDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedGoals.removeAll { it.remoteId == goal.remoteId }
        pushedGoals.add(goal)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun pushProfile(profile: RemoteProfileDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedProfiles.removeAll { it.remoteId == profile.remoteId }
        pushedProfiles.add(profile)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun pushFavorite(favorite: RemoteFavoriteDto): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        pushedFavorites.removeAll { it.remoteId == favorite.remoteId }
        pushedFavorites.add(favorite)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    override suspend fun deleteEntity(entityType: String, remoteId: String): RemoteSyncResult {
        if (shouldFailWithNetworkError) return RemoteSyncResult.NetworkError("Simulated network timeout")
        deletedEntities.add(entityType to remoteId)
        return RemoteSyncResult.Success(System.currentTimeMillis(), versionCounter.incrementAndGet())
    }

    var simulatedPullResponse: RemoteSyncPullResponse = RemoteSyncPullResponse()

    fun setPullResponse(response: RemoteSyncPullResponse) {
        this.simulatedPullResponse = response
    }

    override suspend fun pullChanges(sinceCursor: Long): RemoteSyncPullResponse {
        return simulatedPullResponse
    }
}
