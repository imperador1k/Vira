package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReturnPointType {
    AUTOMATIC_MACHINE,
    KIOSK,
    MANUAL_POINT,
    UNKNOWN
}

enum class ReturnPointSource {
    OFFICIAL,
    COMMUNITY,
    LOCAL_USER
}

enum class VerificationStatus {
    VERIFIED,
    COMMUNITY_CONFIRMED,
    UNVERIFIED,
    DISPUTED,
    REMOVED
}

@Entity(tableName = "return_point")
data class ReturnPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val type: String = ReturnPointType.UNKNOWN.name,
    val source: String = ReturnPointSource.COMMUNITY.name,
    val verificationStatus: String = VerificationStatus.UNVERIFIED.name,
    val openingHours: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long? = null,
    val remoteId: String? = null,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name
)
