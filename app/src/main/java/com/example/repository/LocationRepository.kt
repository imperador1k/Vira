package com.example.repository

import com.example.domain.location.LocationResult

interface LocationRepository {
    suspend fun getCurrentLocation(): LocationResult
    fun hasFineLocationPermission(): Boolean
    fun hasCoarseLocationPermission(): Boolean
    fun isLocationEnabled(): Boolean
}
