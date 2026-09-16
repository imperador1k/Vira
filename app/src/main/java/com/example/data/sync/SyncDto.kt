package com.example.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provider-independent remote DTOs mapped to backend database tables.
 * Separates Room schema from external network schema.
 * Room Entity <-> Domain Model <-> Remote DTO.
 */

@Serializable
data class RemoteCollectionDto(
    @SerialName("id") val remoteId: String,
    @SerialName("container_count") val containerCount: Int,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("estimated_value_cents") val estimatedValueCents: Long,
    @SerialName("spot_id") val spotRemoteId: String? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("client_updated_at") val clientUpdatedAt: Long = timestamp,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteRedemptionDto(
    @SerialName("id") val remoteId: String,
    @SerialName("presented_containers") val presentedContainers: Int,
    @SerialName("accepted_containers") val acceptedContainers: Int,
    @SerialName("rejected_containers") val rejectedContainers: Int = 0,
    @SerialName("actual_recovered_cents") val actualRecoveredCents: Long,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("return_point_id") val returnPointRemoteId: String? = null,
    @SerialName("note") val note: String? = null,
    @SerialName("client_updated_at") val clientUpdatedAt: Long = timestamp,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteSpotDto(
    @SerialName("id") val remoteId: String,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String? = null,
    @SerialName("client_created_at") val clientCreatedAt: Long,
    @SerialName("client_updated_at") val clientUpdatedAt: Long,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteGoalDto(
    @SerialName("id") val remoteId: String,
    @SerialName("type") val type: String,
    @SerialName("target_value") val targetValue: Int,
    @SerialName("period") val period: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("client_updated_at") val clientUpdatedAt: Long,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteProfileDto(
    @SerialName("id") val remoteId: String,
    @SerialName("name") val name: String,
    @SerialName("theme_preference") val themePreference: String,
    @SerialName("client_updated_at") val clientUpdatedAt: Long,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class RemoteFavoriteDto(
    @SerialName("id") val remoteId: String,
    @SerialName("return_point_id") val returnPointId: Int,
    @SerialName("client_created_at") val clientCreatedAt: Long
)

@Serializable
data class RemoteSyncPullResponse(
    @SerialName("collections") val collections: List<RemoteCollectionDto> = emptyList(),
    @SerialName("spots") val spots: List<RemoteSpotDto> = emptyList(),
    @SerialName("redemptions") val redemptions: List<RemoteRedemptionDto> = emptyList(),
    @SerialName("goals") val goals: List<RemoteGoalDto> = emptyList(),
    @SerialName("profile") val profile: RemoteProfileDto? = null,
    @SerialName("new_cursor") val newCursor: Long = 0L,
    @SerialName("has_more") val hasMore: Boolean = false
)

@Serializable
data class PullSyncRpcParams(
    @SerialName("p_since_cursor") val sinceCursor: Long
)
