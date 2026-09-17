package com.example.ui.map

import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.ReturnPointEntity
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.spatialk.geojson.Position

enum class MapFilter {
    All,
    MySpots,
    Collections,
    ReturnPoints
}

enum class MapMode {
    Explore,
    PickLocation
}

data class UserLocationState(
    val position: Position,
    val accuracyMeters: Float? = null,
    val isApproximate: Boolean = false,
    val isCached: Boolean = false
)

data class SpotAggregateInfo(
    val spotId: Int,
    val spotName: String,
    val totalContainers: Int,
    val collectionCount: Int,
    val lastCollectedTimestamp: Long?
)

/**
 * Cluster or individual marker for geolocated collection entries.
 */
data class PersonalCollectionMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val totalContainers: Int,
    val latestCollection: CollectionEntryEntity,
    val collections: List<CollectionEntryEntity>
)

data class MapUiState(
    val cameraPosition: CameraPosition = CameraPosition(
        bearing = 0.0,
        target = Position(longitude = -8.0, latitude = 39.5),
        tilt = 0.0,
        zoom = 6.0
    ),
    val userLocation: UserLocationState? = null,
    val isLocating: Boolean = false,
    val spots: List<CollectionSpotEntity> = emptyList(),
    val collections: List<CollectionEntryEntity> = emptyList(),
    val personalCollectionMarkers: List<PersonalCollectionMarker> = emptyList(),
    val spotAggregates: Map<Int, SpotAggregateInfo> = emptyMap(),
    val returnPoints: List<ReturnPointEntity> = emptyList(),
    val selectedSpot: CollectionSpotEntity? = null,
    val selectedReturnPoint: ReturnPointEntity? = null,
    val selectedCollection: CollectionEntryEntity? = null,
    val mapMode: MapMode = MapMode.Explore,
    val pickedCoordinate: Position? = null,
    val activeFilter: MapFilter = MapFilter.All,
    val searchQuery: String = "",
    val isLocationServicesDisabled: Boolean = false,
    val isOnline: Boolean = true,
    val locationErrorMessage: String? = null,
    val cameraMoveTrigger: Long = 0L
)

