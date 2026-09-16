package com.example.data.sync

/**
 * Provider-independent remote DTOs.
 * Separates Room schema from external network schema.
 * Room Entity <-> Domain Model <-> Remote DTO.
 */

data class RemoteCollectionDto(
    val remoteId: String,
    val containerCount: Int,
    val timestamp: Long,
    val estimatedValueCents: Long,
    val spotRemoteId: String? = null,
    val note: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val clientUpdatedAt: Long = timestamp
)

data class RemoteRedemptionDto(
    val remoteId: String,
    val presentedContainers: Int,
    val acceptedContainers: Int,
    val rejectedContainers: Int,
    val actualRecoveredCents: Long,
    val timestamp: Long,
    val returnPointRemoteId: String? = null,
    val note: String? = null,
    val clientUpdatedAt: Long = timestamp
)

data class RemoteSpotDto(
    val remoteId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val clientCreatedAt: Long,
    val clientUpdatedAt: Long
)

data class RemoteGoalDto(
    val remoteId: String,
    val type: String,
    val targetValue: Int,
    val period: String,
    val isActive: Boolean,
    val clientUpdatedAt: Long
)

data class RemoteProfileDto(
    val remoteId: String,
    val name: String,
    val themePreference: String,
    val clientUpdatedAt: Long
)

data class RemoteFavoriteDto(
    val remoteId: String,
    val returnPointId: Int,
    val clientCreatedAt: Long
)

data class RemoteSyncPullResponse(
    val items: List<Any> = emptyList(),
    val newCursor: Long = 0L,
    val hasMore: Boolean = false
)
