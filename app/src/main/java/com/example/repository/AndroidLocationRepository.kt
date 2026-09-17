package com.example.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.domain.location.LocationResult
import com.example.domain.location.LocationSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidLocationRepository(
    private val context: Context
) : LocationRepository {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        val manager = locationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.IO) {
        val hasFine = hasFineLocationPermission()
        val hasCoarse = hasCoarseLocationPermission()

        if (!hasFine && !hasCoarse) {
            return@withContext LocationResult.PermissionDenied
        }

        if (!isLocationEnabled()) {
            return@withContext LocationResult.LocationDisabled
        }

        val priority = if (hasFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        try {
            // Request fresh current location fix with a 10-second timeout
            val location = withTimeoutOrNull(10_000L) {
                requestFusedLocation(priority)
            }

            if (location != null) {
                return@withContext mapToResult(location, hasFine, LocationSource.FRESH)
            }

            // Fallback to native LocationManager if fused client times out or returns null
            val fallbackLocation = getFallbackLocation()
            if (fallbackLocation != null) {
                return@withContext mapToResult(fallbackLocation, hasFine, LocationSource.CACHED)
            }

            LocationResult.Unavailable
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location fix", e)
            LocationResult.Error(e.localizedMessage ?: "Unknown location error")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedLocation(priority: Int): Location? {
        val cts = CancellationTokenSource()
        return suspendCancellableCoroutine { continuation ->
            fusedClient.getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    continuation.resume(loc)
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Fused location provider failed, trying fallback", error)
                    continuation.resume(null)
                }
            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    internal fun getFallbackLocation(
        maxAgeMillis: Long = MAX_FALLBACK_AGE_MILLIS,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Location? {
        val manager = locationManager ?: return null
        return try {
            val gpsLoc = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLoc = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val candidate = when {
                gpsLoc != null && networkLoc != null -> {
                    if (gpsLoc.time >= networkLoc.time) gpsLoc else networkLoc
                }
                gpsLoc != null -> gpsLoc
                else -> networkLoc
            } ?: return null

            val age = currentTimeMillis - candidate.time
            // Accept only if age is within maxAgeMillis (allowing up to 60s negative clock skew)
            if (age in -60_000L..maxAgeMillis) {
                candidate
            } else {
                Log.d(TAG, "Rejected stale fallback location (age: ${age / 1000}s, max: ${maxAgeMillis / 1000}s)")
                null
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException during fallback location access", e)
            null
        }
    }

    private fun mapToResult(
        location: Location,
        hasFinePermission: Boolean,
        source: LocationSource = LocationSource.FRESH
    ): LocationResult {
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        return if (hasFinePermission) {
            LocationResult.Success(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = accuracy,
                source = source
            )
        } else {
            LocationResult.ApproximateOnly(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = accuracy,
                source = source
            )
        }
    }

    companion object {
        private const val TAG = "AndroidLocationRepo"
        const val MAX_FALLBACK_AGE_MILLIS = 5 * 60 * 1000L // 5 minutes
    }
}
