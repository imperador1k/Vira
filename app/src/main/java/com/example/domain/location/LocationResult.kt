package com.example.domain.location

enum class LocationSource {
    FRESH,
    CACHED
}

sealed interface LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null,
        val source: LocationSource = LocationSource.FRESH
    ) : LocationResult

    data class ApproximateOnly(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null,
        val source: LocationSource = LocationSource.FRESH
    ) : LocationResult

    object PermissionDenied : LocationResult
    object LocationDisabled : LocationResult
    object Timeout : LocationResult
    object Unavailable : LocationResult
    data class Error(val message: String) : LocationResult
}
