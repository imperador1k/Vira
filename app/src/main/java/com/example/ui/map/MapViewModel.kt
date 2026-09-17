package com.example.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.ReturnPointEntity
import com.example.domain.location.LocationResult
import com.example.domain.location.LocationSource
import com.example.repository.LocationRepository
import com.example.repository.ReturnPointRepository
import com.example.repository.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.spatialk.geojson.Position

class MapViewModel(
    private val spotRepository: SpotRepository,
    private val returnPointRepository: ReturnPointRepository,
    private val locationRepository: LocationRepository,
    private val networkMonitor: com.example.util.NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var hasInitializedCamera = false

    init {
        checkLocationServicesState()

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }

        viewModelScope.launch {
            returnPointRepository.seedDefaultReturnPointsIfEmpty()
        }

        viewModelScope.launch {
            combine(
                spotRepository.getAllSpots(),
                returnPointRepository.getAllReturnPoints()
            ) { spots, returnPoints ->
                Pair(spots, returnPoints)
            }.collect { (spots, returnPoints) ->
                _uiState.update { current ->
                    current.copy(
                        spots = spots,
                        returnPoints = returnPoints
                    )
                }
            }
        }
    }

    fun checkLocationServicesState() {
        val enabled = locationRepository.isLocationEnabled()
        _uiState.update {
            it.copy(
                isLocationServicesDisabled = !enabled,
                locationErrorMessage = if (!enabled) "Ativa a localização para utilizar a tua posição atual." else if (it.isLocationServicesDisabled) null else it.locationErrorMessage
            )
        }
    }

    fun updateCameraPosition(newPosition: CameraPosition) {
        hasInitializedCamera = true
        _uiState.update { it.copy(cameraPosition = newPosition) }
    }

    fun requestCurrentLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLocating = true,
                    isLocationServicesDisabled = false,
                    locationErrorMessage = null
                )
            }

            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val isCached = result.source == LocationSource.CACHED
                    val pos = Position(longitude = result.longitude, latitude = result.latitude)
                    hasInitializedCamera = true
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            userLocation = UserLocationState(
                                position = pos,
                                accuracyMeters = result.accuracyMeters,
                                isApproximate = false,
                                isCached = isCached
                            ),
                            cameraPosition = CameraPosition(
                                bearing = 0.0,
                                target = pos,
                                tilt = 0.0,
                                zoom = if (isCached) 14.5 else 15.5
                            ),
                            cameraMoveTrigger = it.cameraMoveTrigger + 1,
                            locationErrorMessage = null
                        )
                    }
                }
                is LocationResult.ApproximateOnly -> {
                    val isCached = result.source == LocationSource.CACHED
                    val pos = Position(longitude = result.longitude, latitude = result.latitude)
                    hasInitializedCamera = true
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            userLocation = UserLocationState(
                                position = pos,
                                accuracyMeters = result.accuracyMeters,
                                isApproximate = true,
                                isCached = isCached
                            ),
                            cameraPosition = CameraPosition(
                                bearing = 0.0,
                                target = pos,
                                tilt = 0.0,
                                zoom = 14.0
                            ),
                            cameraMoveTrigger = it.cameraMoveTrigger + 1,
                            locationErrorMessage = null
                        )
                    }
                }
                is LocationResult.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            isLocationServicesDisabled = true,
                            locationErrorMessage = "Ativa a localização do dispositivo para encontrares a tua posição."
                        )
                    }
                }
                is LocationResult.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationErrorMessage = "Permissão de localização necessária para centrar a posição."
                        )
                    }
                }
                is LocationResult.Timeout -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationErrorMessage = "Tempo limite excedido ao obter sinal de GPS."
                        )
                    }
                }
                is LocationResult.Unavailable -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationErrorMessage = "Sinal de localização indisponível de momento."
                        )
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun recenterOnUser() {
        val userLoc = _uiState.value.userLocation ?: return
        _uiState.update {
            it.copy(
                cameraPosition = CameraPosition(
                    bearing = 0.0,
                    target = userLoc.position,
                    tilt = 0.0,
                    zoom = 15.5
                ),
                cameraMoveTrigger = it.cameraMoveTrigger + 1
            )
        }
    }

    fun selectSpot(spot: CollectionSpotEntity?) {
        _uiState.update {
            it.copy(
                selectedSpot = spot,
                selectedReturnPoint = null
            )
        }
    }

    fun selectReturnPoint(point: ReturnPointEntity?) {
        _uiState.update {
            it.copy(
                selectedReturnPoint = point,
                selectedSpot = null
            )
        }
    }

    fun setFilter(filter: MapFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMapMode(mode: MapMode) {
        _uiState.update { it.copy(mapMode = mode) }
    }

    fun setPickedCoordinate(pos: Position) {
        _uiState.update { it.copy(pickedCoordinate = pos) }
    }

    fun clearErrorMessage() {
        _uiState.update {
            it.copy(
                locationErrorMessage = null,
                isLocationServicesDisabled = false
            )
        }
    }
}

class MapViewModelFactory(
    private val spotRepository: SpotRepository,
    private val returnPointRepository: ReturnPointRepository,
    private val locationRepository: LocationRepository,
    private val networkMonitor: com.example.util.NetworkMonitor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(
                spotRepository,
                returnPointRepository,
                locationRepository,
                networkMonitor
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
