package com.example.data.sync

/**
 * Provider-independent remote synchronization contract.
 * Does not depend on Supabase, Firebase, or any cloud vendor SDK.
 */
interface SyncRemoteDataSource {
    suspend fun pushCollection(entry: RemoteCollectionDto): RemoteSyncResult
    suspend fun pushRedemption(entry: RemoteRedemptionDto): RemoteSyncResult
    suspend fun pushSpot(spot: RemoteSpotDto): RemoteSyncResult
    suspend fun pushGoal(goal: RemoteGoalDto): RemoteSyncResult
    suspend fun pushProfile(profile: RemoteProfileDto): RemoteSyncResult
    suspend fun pushFavorite(favorite: RemoteFavoriteDto): RemoteSyncResult
    suspend fun deleteEntity(entityType: String, remoteId: String): RemoteSyncResult

    suspend fun pullChanges(sinceCursor: Long): RemoteSyncPullResponse
}
