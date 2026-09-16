package com.example

import com.example.domain.location.LocationResult
import com.example.repository.LocationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRepositoryTest {

    class FakeLocationRepository(
        var fineGranted: Boolean = true,
        var coarseGranted: Boolean = true,
        var locationEnabled: Boolean = true,
        var simulatedResult: LocationResult = LocationResult.Success(38.7223, -9.1393, 10f)
    ) : LocationRepository {
        override suspend fun getCurrentLocation(): LocationResult {
            if (!fineGranted && !coarseGranted) return LocationResult.PermissionDenied
            if (!locationEnabled) return LocationResult.LocationDisabled
            return simulatedResult
        }

        override fun hasFineLocationPermission(): Boolean = fineGranted
        override fun hasCoarseLocationPermission(): Boolean = coarseGranted
        override fun isLocationEnabled(): Boolean = locationEnabled
    }

    @Test
    fun returnsSuccessWhenLocationIsAvailable() = runTest {
        val repo = FakeLocationRepository()
        val result = repo.getCurrentLocation()
        assertTrue(result is LocationResult.Success)
        val success = result as LocationResult.Success
        assertEquals(38.7223, success.latitude, 0.0001)
        assertEquals(-9.1393, success.longitude, 0.0001)
    }

    @Test
    fun returnsPermissionDeniedWhenNoPermissionsGranted() = runTest {
        val repo = FakeLocationRepository(fineGranted = false, coarseGranted = false)
        val result = repo.getCurrentLocation()
        assertEquals(LocationResult.PermissionDenied, result)
    }

    @Test
    fun returnsLocationDisabledWhenSystemLocationIsOff() = runTest {
        val repo = FakeLocationRepository(locationEnabled = false)
        val result = repo.getCurrentLocation()
        assertEquals(LocationResult.LocationDisabled, result)
    }

    @Test
    fun returnsApproximateOnlyWhenFineLocationNotGranted() = runTest {
        val repo = FakeLocationRepository(
            fineGranted = false,
            coarseGranted = true,
            simulatedResult = LocationResult.ApproximateOnly(38.72, -9.14, 1500f)
        )
        val result = repo.getCurrentLocation()
        assertTrue(result is LocationResult.ApproximateOnly)
    }
}
