package com.example.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ViraBackupMetadata(
    @SerialName("format_version") val formatVersion: Int = 1,
    @SerialName("app_version") val appVersion: String = "0.2.0-beta",
    @SerialName("created_at") val createdAt: String,
    @SerialName("device_model") val deviceModel: String = "",
    @SerialName("collections_count") val collectionsCount: Int = 0,
    @SerialName("spots_count") val spotsCount: Int = 0,
    @SerialName("redemptions_count") val redemptionsCount: Int = 0,
    @SerialName("goals_count") val goalsCount: Int = 0,
    @SerialName("total_containers") val totalContainers: Int = 0,
    @SerialName("checksum_sha256") val checksumSha256: String = ""
)

@Serializable
data class ViraBackupSpot(
    @SerialName("client_key") val clientKey: String,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("last_visited_at") val lastVisitedAt: Long? = null,
    @SerialName("total_visits") val totalVisits: Int = 0,
    @SerialName("lifetime_containers") val lifetimeContainers: Int = 0,
    @SerialName("average_containers_per_visit") val averageContainersPerVisit: Double = 0.0
)

@Serializable
data class ViraBackupCollection(
    @SerialName("client_key") val clientKey: String,
    @SerialName("spot_client_key") val spotClientKey: String? = null,
    @SerialName("container_count") val containerCount: Int,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("estimated_value_cents") val estimatedValueCents: Long,
    @SerialName("note") val note: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class ViraBackupRedemption(
    @SerialName("client_key") val clientKey: String,
    @SerialName("presented_containers") val presentedContainers: Int,
    @SerialName("accepted_containers") val acceptedContainers: Int,
    @SerialName("rejected_containers") val rejectedContainers: Int,
    @SerialName("actual_recovered_cents") val actualRecoveredCents: Long,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("note") val note: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class ViraBackupGoal(
    @SerialName("type") val type: String,
    @SerialName("target_value") val targetValue: Int,
    @SerialName("period") val period: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long
)

@Serializable
data class ViraBackupProfile(
    @SerialName("display_name") val displayName: String,
    @SerialName("username") val username: String = "",
    @SerialName("city") val city: String = ""
)

@Serializable
data class ViraBackupPayload(
    @SerialName("metadata") val metadata: ViraBackupMetadata,
    @SerialName("spots") val spots: List<ViraBackupSpot> = emptyList(),
    @SerialName("collections") val collections: List<ViraBackupCollection> = emptyList(),
    @SerialName("redemptions") val redemptions: List<ViraBackupRedemption> = emptyList(),
    @SerialName("goals") val goals: List<ViraBackupGoal> = emptyList(),
    @SerialName("profile") val profile: ViraBackupProfile? = null
)

@Serializable
data class CloudBackupItem(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("collections_count") val collectionsCount: Int = 0,
    @SerialName("spots_count") val spotsCount: Int = 0,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("backup_type") val backupType: String = "MANUAL",
    @SerialName("payload") val payload: ViraBackupPayload? = null
)
