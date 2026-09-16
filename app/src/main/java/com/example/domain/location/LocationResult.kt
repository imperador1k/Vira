package com.example.domain.location

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null
    ) : LocationResult

    data class ApproximateOnly(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null
    ) : LocationResult

    object PermissionDenied : LocationResult
    object LocationDisabled : LocationResult
    object Timeout : LocationResult
    object Unavailable : LocationResult
    data class Error(val message: String) : LocationResult
}
