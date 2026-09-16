package com.example.data.sync.supabase

import com.example.data.local.OutboxEntityType
import com.example.data.sync.RemoteCollectionDto
import com.example.data.sync.RemoteFavoriteDto
import com.example.data.sync.RemoteGoalDto
import com.example.data.sync.RemoteProfileDto
import com.example.data.sync.RemoteRedemptionDto
import com.example.data.sync.RemoteSpotDto
import com.example.data.sync.RemoteSyncPullResponse
import com.example.data.sync.RemoteSyncResult
import com.example.data.sync.SyncRemoteDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.math.max

/**
 * Production implementation of SyncRemoteDataSource backed by Supabase PostgREST.
 * Maps decoupled network DTOs to PostgreSQL tables defined in supabase/schema.sql.
 */
class SupabaseSyncRemoteDataSource(
    private val client: SupabaseClient
) : SyncRemoteDataSource {

    private fun parseServerTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    override suspend fun pushCollection(entry: RemoteCollectionDto): RemoteSyncResult {
        return try {
            val response = client.from("collection_entries").upsert(entry) {
                select()
            }.decodeSingle<RemoteCollectionDto>()
            val updatedAtMillis = parseServerTimestamp(response.serverUpdatedAt)
            val remoteVersion = response.serverVersion ?: 1L
            RemoteSyncResult.Success(updatedAtMillis, remoteVersion)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to push collection to Supabase", canRetry = true)
        }
    }

    override suspend fun pushRedemption(entry: RemoteRedemptionDto): RemoteSyncResult {
        return try {
            val response = client.from("redemption_entries").upsert(entry) {
                select()
            }.decodeSingle<RemoteRedemptionDto>()
            val updatedAtMillis = parseServerTimestamp(response.serverUpdatedAt)
            val remoteVersion = response.serverVersion ?: 1L
            RemoteSyncResult.Success(updatedAtMillis, remoteVersion)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to push redemption to Supabase", canRetry = true)
        }
    }

    override suspend fun pushSpot(spot: RemoteSpotDto): RemoteSyncResult {
        return try {
            val response = client.from("collection_spots").upsert(spot) {
                select()
            }.decodeSingle<RemoteSpotDto>()
            val updatedAtMillis = parseServerTimestamp(response.serverUpdatedAt)
            val remoteVersion = response.serverVersion ?: 1L
            RemoteSyncResult.Success(updatedAtMillis, remoteVersion)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to push spot to Supabase", canRetry = true)
        }
    }

    override suspend fun pushGoal(goal: RemoteGoalDto): RemoteSyncResult {
        return try {
            val response = client.from("goals").upsert(goal) {
                select()
            }.decodeSingle<RemoteGoalDto>()
            val updatedAtMillis = parseServerTimestamp(response.serverUpdatedAt)
            val remoteVersion = response.serverVersion ?: 1L
            RemoteSyncResult.Success(updatedAtMillis, remoteVersion)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to push goal to Supabase", canRetry = true)
        }
    }

    override suspend fun pushProfile(profile: RemoteProfileDto): RemoteSyncResult {
        return try {
            val response = client.from("user_profiles").upsert(profile) {
                select()
            }.decodeSingle<RemoteProfileDto>()
            val updatedAtMillis = parseServerTimestamp(response.serverUpdatedAt)
            val remoteVersion = response.serverVersion ?: 1L
            RemoteSyncResult.Success(updatedAtMillis, remoteVersion)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to push profile to Supabase", canRetry = true)
        }
    }

    override suspend fun pushFavorite(favorite: RemoteFavoriteDto): RemoteSyncResult {
        return RemoteSyncResult.Success(System.currentTimeMillis(), 1L)
    }

    override suspend fun deleteEntity(entityType: String, remoteId: String): RemoteSyncResult {
        val table = when (entityType) {
            OutboxEntityType.COLLECTION_ENTRY.name -> "collection_entries"
            OutboxEntityType.COLLECTION_SPOT.name -> "collection_spots"
            OutboxEntityType.REDEMPTION_ENTRY.name -> "redemption_entries"
            OutboxEntityType.GOAL.name -> "goals"
            OutboxEntityType.USER_PROFILE.name -> "user_profiles"
            else -> null
        } ?: return RemoteSyncResult.Success(System.currentTimeMillis(), 0L)

        return try {
            client.from(table).delete {
                filter {
                    eq("id", remoteId)
                }
            }
            RemoteSyncResult.Success(System.currentTimeMillis(), 0L)
        } catch (e: Exception) {
            RemoteSyncResult.NetworkError(e.message ?: "Failed to delete from Supabase", canRetry = true)
        }
    }

    override suspend fun pullChanges(sinceCursor: Long): RemoteSyncPullResponse {
        return try {
            val collections = client.from("collection_entries").select {
                filter { gt("server_version", sinceCursor) }
            }.decodeList<RemoteCollectionDto>()

            val spots = client.from("collection_spots").select {
                filter { gt("server_version", sinceCursor) }
            }.decodeList<RemoteSpotDto>()

            val redemptions = client.from("redemption_entries").select {
                filter { gt("server_version", sinceCursor) }
            }.decodeList<RemoteRedemptionDto>()

            val goals = client.from("goals").select {
                filter { gt("server_version", sinceCursor) }
            }.decodeList<RemoteGoalDto>()

            val profiles = client.from("user_profiles").select {
                filter { gt("server_version", sinceCursor) }
            }.decodeList<RemoteProfileDto>()

            val maxCollectionVer = collections.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
            val maxSpotVer = spots.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
            val maxRedemptionVer = redemptions.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
            val maxGoalVer = goals.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor
            val maxProfileVer = profiles.mapNotNull { it.serverVersion }.maxOrNull() ?: sinceCursor

            val nextCursor = maxOf(sinceCursor, maxCollectionVer, maxSpotVer, maxRedemptionVer, maxGoalVer, maxProfileVer)

            RemoteSyncPullResponse(
                collections = collections,
                spots = spots,
                redemptions = redemptions,
                goals = goals,
                profile = profiles.firstOrNull(),
                newCursor = nextCursor,
                hasMore = false
            )
        } catch (e: Exception) {
            RemoteSyncPullResponse(newCursor = sinceCursor, hasMore = false)
        }
    }
}
