package com.example.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.example.domain.location.LocationResult
import com.example.domain.location.LocationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StaleLocationFallbackPolicyTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var repository: AndroidLocationRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)
        repository = AndroidLocationRepository(context)
    }

    @Test
    fun freshLocation_isDistinguishedAsFreshSource() {
        val freshResult = LocationResult.Success(
            latitude = 39.602,
            longitude = -8.409,
            accuracyMeters = 5f,
            source = LocationSource.FRESH
        )

        assertEquals(LocationSource.FRESH, freshResult.source)
    }

    @Test
    fun cachedLocationUnder5Minutes_isAcceptedAsCached() {
        val now = System.currentTimeMillis()
        val recentLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 39.602
            longitude = -8.409
            accuracy = 10f
            time = now - (2 * 60 * 1000L) // 2 minutes ago (< 5 min)
        }

        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, recentLocation)

        val fallback = repository.getFallbackLocation(
            maxAgeMillis = AndroidLocationRepository.MAX_FALLBACK_AGE_MILLIS,
            currentTimeMillis = now
        )

        assertNotNull("Recent location under 5 minutes must be accepted", fallback)
        assertEquals(39.602, fallback!!.latitude, 0.0001)
        assertEquals(-8.409, fallback.longitude, 0.0001)
    }

    @Test
    fun cachedLocationOver5Minutes_isRejected() {
        val now = System.currentTimeMillis()
        val staleLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 39.602
            longitude = -8.409
            accuracy = 10f
            time = now - (6 * 60 * 1000L) // 6 minutes ago (> 5 min)
        }

        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, staleLocation)

        val fallback = repository.getFallbackLocation(
            maxAgeMillis = AndroidLocationRepository.MAX_FALLBACK_AGE_MILLIS,
            currentTimeMillis = now
        )

        assertNull("Stale location over 5 minutes must be rejected as fallback", fallback)
    }

    @Test
    fun noLocationAvailable_returnsNullFallback() {
        val now = System.currentTimeMillis()
        val fallback = repository.getFallbackLocation(
            maxAgeMillis = AndroidLocationRepository.MAX_FALLBACK_AGE_MILLIS,
            currentTimeMillis = now
        )

        assertNull("No fallback location should be returned when providers are empty", fallback)
    }
}
