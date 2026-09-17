package com.example.ui.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.ReturnPointEntity
import com.example.ui.components.ViraIconButton
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraSpotRow
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.home.CollectionBottomSheet
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.ZoomButtons
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToSpot: (Int) -> Unit = {},
    initialMapMode: MapMode = MapMode.Explore,
    onLocationPicked: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            spotRepository = appContainer.spotRepository,
            returnPointRepository = appContainer.returnPointRepository,
            locationRepository = appContainer.locationRepository,
            networkMonitor = appContainer.networkMonitor
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationProvider = rememberDefaultLocationProvider()
    val locationState = rememberLocationState(provider = locationProvider)

    // Enforce map lifecycle strictly: MapLibre must leave the visual tree when route is not RESUMED
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isMapRouteActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    LaunchedEffect(isMapRouteActive) {
        if (isMapRouteActive) {
            viewModel.checkLocationServicesState()
        }
    }

    var isListView by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var customPickedPos by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val collectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    val styleUrl = MapConfig.getStyleUrl()
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = uiState.cameraPosition
    ) {
        LocationPuck(
            idPrefix = "user",
            locationState = locationState
        )
    }

    // Sync initial map mode if launched in picking mode
    LaunchedEffect(initialMapMode) {
        if (initialMapMode == MapMode.PickLocation) {
            viewModel.setMapMode(MapMode.PickLocation)
        }
    }

    // Sync programmatic camera movements from ViewModel to MapState
    LaunchedEffect(uiState.cameraPosition, uiState.cameraMoveTrigger) {
        if (uiState.cameraMoveTrigger > 0L) {
            mapState.animateCameraPosition(uiState.cameraPosition)
        }
    }

    fun requestGpsFix() {
        if (permissionsState.allPermissionsGranted || permissionsState.permissions.any { it.status.isGranted }) {
            uiState.userLocation?.let {
                viewModel.recenterOnUser()
            }
            viewModel.requestCurrentLocation()
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Filter spots and return points
    val filteredSpots = uiState.spots.filter {
        if (uiState.searchQuery.isBlank()) true
        else it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                (it.address?.contains(uiState.searchQuery, ignoreCase = true) == true)
    }

    val filteredReturnPoints = uiState.returnPoints.filter {
        if (uiState.searchQuery.isBlank()) true
        else it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                (it.address?.contains(uiState.searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isListView) {
            // LIST VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = ViraSpacing.space24),
                contentPadding = PaddingValues(bottom = ViraSpacing.space48)
            ) {
                item {
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Locais", style = ViraTypography.MetricLarge)
                        ViraIconButton(
                            icon = Icons.Default.Map,
                            contentDescription = "Ver no mapa",
                            onClick = { isListView = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    ViraSectionHeader(title = "SPOTS DE RECOLHA (${filteredSpots.size})")
                    Spacer(modifier = Modifier.height(ViraSpacing.space12))
                }

                items(filteredSpots) { spot ->
                    ViraSpotRow(
                        name = spot.name,
                        totalContainers = spot.lifetimeContainers,
                        averagePerVisit = spot.averageContainersPerVisit,
                        lastVisitText = if (spot.lastVisitedAt != null) "recente" else "sem registo",
                        onClick = {
                            viewModel.selectSpot(spot)
                            isListView = false
                        }
                    )
                }

                if (filteredReturnPoints.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                        ViraSectionHeader(title = "POSTOS DE DEVOLUÇÃO (${filteredReturnPoints.size})")
                        Spacer(modifier = Modifier.height(ViraSpacing.space12))
                    }

                    items(filteredReturnPoints) { point ->
                        ViraSurfaceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = ViraSpacing.space4)
                                .clickable {
                                    viewModel.selectReturnPoint(point)
                                    isListView = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = point.name, style = ViraTypography.ButtonLabel)
                                    point.address?.let {
                                        Text(text = it, style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                val isVerified = point.verificationStatus == "VERIFIED"
                                Surface(
                                    shape = RoundedCornerShape(ViraRadius.small),
                                    color = if (isVerified) Color(0xFF0F2E28) else Color(0xFF2A2416)
                                ) {
                                    Text(
                                        text = if (isVerified) "VERIFICADO" else "COMUNITÁRIO",
                                        style = ViraTypography.Caption,
                                        color = if (isVerified) Color(0xFF00D1B2) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                        ViraSectionHeader(title = "POSTOS DE DEVOLUÇÃO")
                        Spacer(modifier = Modifier.height(ViraSpacing.space12))
                        ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Pontos de devolução ainda não disponíveis nesta versão.",
                                style = ViraTypography.BodySecondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // MAP HERO VIEW
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // RENDER MAPLIBRE ONLY WHEN ACTIVE DESTINATION TO PREVENT GL SURFACE BLEED
                if (isMapRouteActive) {
                    MaplibreMap(
                        modifier = Modifier.fillMaxSize(),
                        state = mapState
                    ) {
                        // 1. Official ZoomButtons
                        ZoomButtons(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = ViraSpacing.space16),
                            onZoomIn = {
                                val currentPos = mapState.cameraPosition
                                val targetZoom = (currentPos.zoom + 1.0).coerceAtMost(20.0)
                                scope.launch {
                                    mapState.animateCameraPosition(currentPos.copy(zoom = targetZoom))
                                }
                            },
                            onZoomOut = {
                                val currentPos = mapState.cameraPosition
                                val targetZoom = (currentPos.zoom - 1.0).coerceAtLeast(1.0)
                                scope.launch {
                                    mapState.animateCameraPosition(currentPos.copy(zoom = targetZoom))
                                }
                            }
                        )



                        // Current User Location Fallback Marker if LocationPuck hasn't received its first fix
                        if (locationState.lastLocation == null) {
                            uiState.userLocation?.let { loc ->
                                val ringColor = if (loc.isCached) Color(0xFFF59E0B) else Color(0xFF00D1B2)
                                Box(
                                    modifier = Modifier.placedAt(position = loc.position, alignment = Alignment.Center)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (loc.isApproximate || loc.isCached) 44.dp else 28.dp)
                                            .clip(CircleShape)
                                            .background(ringColor.copy(alpha = if (loc.isApproximate || loc.isCached) 0.18f else 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(ringColor)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Personal Spots (Vira Cyan)
                        if (uiState.activeFilter == MapFilter.All || uiState.activeFilter == MapFilter.MySpots) {
                            filteredSpots.forEach { spot ->
                                val isSelected = uiState.selectedSpot?.id == spot.id
                                val position = Position(longitude = spot.longitude, latitude = spot.latitude)
                                Box(
                                    modifier = Modifier
                                        .placedAt(position = position, alignment = Alignment.Center)
                                        .clickable { viewModel.selectSpot(spot) }
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00D1B2).copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00D1B2))
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00D1B2))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Return Points (Verified vs Community)
                        if (uiState.activeFilter == MapFilter.All || uiState.activeFilter == MapFilter.ReturnPoints) {
                            filteredReturnPoints.forEach { point ->
                                val isSelected = uiState.selectedReturnPoint?.id == point.id
                                val position = Position(longitude = point.longitude, latitude = point.latitude)
                                val isVerified = point.verificationStatus == "VERIFIED"

                                Box(
                                    modifier = Modifier
                                        .placedAt(position = position, alignment = Alignment.Center)
                                        .clickable { viewModel.selectReturnPoint(point) }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isVerified) Color(0xFF0F2E28) else Color(0xFF2A2416),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isVerified) Color(0xFF00D1B2) else Color(0xFFF59E0B)
                                        ),
                                        shadowElevation = if (isSelected) 8.dp else 2.dp
                                    ) {
                                        Icon(
                                            imageVector = if (isVerified) Icons.Default.Recycling else Icons.Default.Place,
                                            contentDescription = point.name,
                                            tint = if (isVerified) Color(0xFF00D1B2) else Color(0xFFF59E0B),
                                            modifier = Modifier
                                                .size(if (isSelected) 26.dp else 20.dp)
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // LOCATION SELECTION MODE ("Escolher no mapa") CENTER CROSSHAIR
                if (uiState.mapMode == MapMode.PickLocation) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Mira de seleção",
                            tint = Color(0xFF00D1B2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Bottom Confirmation Bar for PickLocation
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(ViraSpacing.space16),
                        shape = RoundedCornerShape(ViraRadius.large),
                        color = LocalViraExtraColors.current.surfaceElevated,
                        shadowElevation = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(ViraSpacing.space16)) {
                            val target = mapState.cameraPosition.target
                            Text(text = "Local selecionado", style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format("%.4f", target.latitude)}, ${String.format("%.4f", target.longitude)}",
                                style = ViraTypography.MetricMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)) {
                                TextButton(
                                    onClick = { viewModel.setMapMode(MapMode.Explore) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar", style = ViraTypography.ButtonLabel)
                                }
                                ViraPrimaryButton(
                                    text = "Selecionar este local",
                                    onClick = {
                                        customPickedPos = Pair(target.latitude, target.longitude)
                                        onLocationPicked?.invoke(target.latitude, target.longitude)
                                        viewModel.setMapMode(MapMode.Explore)
                                        showCollectionSheet = true
                                    },
                                    modifier = Modifier.weight(2f)
                                )
                            }
                        }
                    }
                }

                // FLOATING SEARCH & FILTER SURFACE (Explore mode)
                if (uiState.mapMode == MapMode.Explore) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space12)
                    ) {
                        // Search bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(ViraRadius.large))
                                .clip(RoundedCornerShape(ViraRadius.large))
                                .background(LocalViraExtraColors.current.surfaceElevated)
                                .padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space12))
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Procurar localização...", style = ViraTypography.Body) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                            ViraIconButton(
                                icon = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Vista de lista",
                                onClick = { isListView = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(ViraSpacing.space8))

                        // Filter chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)
                        ) {
                            listOf(
                                MapFilter.All to "Todos",
                                MapFilter.MySpots to "Meus spots",
                                MapFilter.ReturnPoints to "Devolução"
                            ).forEach { (filter, label) ->
                                val isSelected = uiState.activeFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setFilter(filter) },
                                    label = { Text(label, style = ViraTypography.Caption) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Honest Return Points Empty Notice when ReturnPoints filter is active
                        if (uiState.activeFilter == MapFilter.ReturnPoints && filteredReturnPoints.isEmpty()) {
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Pontos de devolução ainda não disponíveis nesta versão.",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space12)
                                )
                            }
                        }

                        // Non-blocking Offline Banner
                        if (!uiState.isOnline) {
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(ViraSpacing.space12),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(ViraSpacing.space12))
                                        Column {
                                            Text(
                                                text = "Sem ligação à Internet",
                                                style = ViraTypography.ButtonLabel,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "O mapa necessita de ligação para carregar.\nAs tuas recolhas continuam disponíveis offline.",
                                                style = ViraTypography.Caption,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                    TextButton(
                                        onClick = {
                                            viewModel.recenterOnUser()
                                        }
                                    ) {
                                        Text("Tentar novamente", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Actionable Location Disabled Banner
                        if (uiState.isLocationServicesDisabled) {
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(ViraSpacing.space12),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Localização desativada",
                                            style = ViraTypography.ButtonLabel,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Ativa a localização para utilizar a tua posição atual.",
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                    TextButton(
                                        onClick = {
                                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            viewModel.clearErrorMessage()
                                        }
                                    ) {
                                        Text("Ativar localização", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        // Informative Cached Fallback Notice
                        if (uiState.userLocation?.isCached == true) {
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space8),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                    Text(
                                        text = "Posição recente em cache (< 5 min). Sem sinal GPS direto.",
                                        style = ViraTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Informative Unavailable / Error Notice
                        if (uiState.locationErrorMessage != null && !uiState.isLocationServicesDisabled) {
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space8),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                    Text(
                                        text = uiState.locationErrorMessage!!,
                                        style = ViraTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // FLOATING GPS BUTTON WITH LOADING STATE
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                bottom = if (uiState.selectedSpot != null || uiState.selectedReturnPoint != null) 270.dp else 32.dp,
                                end = ViraSpacing.space16
                            )
                    ) {
                        Surface(
                            onClick = { requestGpsFix() },
                            shape = CircleShape,
                            color = LocalViraExtraColors.current.surfaceElevated,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (uiState.isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "A minha localização",
                                        tint = if (uiState.userLocation != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // SLIDING PARTIAL BOTTOM SHEET: Personal Spot Selected
                    AnimatedVisibility(
                        visible = uiState.selectedSpot != null,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        uiState.selectedSpot?.let { spot ->
                            ViraSurfaceCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(ViraSpacing.space16)
                                    .shadow(12.dp, RoundedCornerShape(ViraRadius.large))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = spot.name, style = ViraTypography.MetricMedium)
                                    IconButton(onClick = { viewModel.selectSpot(null) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                                    }
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                Text(
                                    text = "${String.format("%.1f", spot.averageContainersPerVisit)} embalagens / visita",
                                    style = ViraTypography.BodySecondary,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                Text(
                                    text = "${spot.lifetimeContainers} recolhidas · ${spot.totalVisits} visitas",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(ViraSpacing.space16))

                                ViraPrimaryButton(
                                    text = "+ Registar aqui",
                                    onClick = { showCollectionSheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(ViraSpacing.space8))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onNavigateToSpot(spot.id) }) {
                                        Text("Ver detalhes", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(
                                        onClick = {
                                            val gmmIntentUri = Uri.parse("geo:${spot.latitude},${spot.longitude}?q=${spot.latitude},${spot.longitude}(${Uri.encode(spot.name)})")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Como chegar", style = ViraTypography.ButtonLabel)
                                    }
                                }
                            }
                        }
                    }

                    // SLIDING PARTIAL BOTTOM SHEET: Return Point Selected
                    AnimatedVisibility(
                        visible = uiState.selectedReturnPoint != null,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        uiState.selectedReturnPoint?.let { point ->
                            val isVerified = point.verificationStatus == "VERIFIED"
                            ViraSurfaceCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(ViraSpacing.space16)
                                    .shadow(12.dp, RoundedCornerShape(ViraRadius.large))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = point.name, style = ViraTypography.MetricMedium)
                                        point.address?.let {
                                            Text(text = it, style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.selectReturnPoint(null) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                                    }
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                                Surface(
                                    shape = RoundedCornerShape(ViraRadius.small),
                                    color = if (isVerified) Color(0xFF0F2E28) else Color(0xFF2A2416)
                                ) {
                                    Text(
                                        text = if (isVerified) "POSTO OFICIAL SDR (VERIFICADO)" else "PONTO COMUNITÁRIO (NÃO VERIFICADO)",
                                        style = ViraTypography.Caption,
                                        color = if (isVerified) Color(0xFF00D1B2) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                point.openingHours?.let {
                                    Spacer(modifier = Modifier.height(ViraSpacing.space8))
                                    Text(text = "Horário: $it", style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space16))

                                TextButton(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${Uri.encode(point.name)})")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        context.startActivity(mapIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Como chegar", style = ViraTypography.ButtonLabel)
                                }
                            }
                        }
                    }
                }
            }
        }

        // COLLECTION MODAL BOTTOM SHEET
        if (showCollectionSheet) {
            CollectionBottomSheet(
                sheetState = collectionSheetState,
                spots = uiState.spots,
                initialSpotId = uiState.selectedSpot?.id,
                pickedCoordinate = customPickedPos,
                onPickOnMap = {
                    scope.launch { collectionSheetState.hide() }.invokeOnCompletion {
                        showCollectionSheet = false
                        viewModel.setMapMode(MapMode.PickLocation)
                    }
                },
                onDismiss = {
                    scope.launch { collectionSheetState.hide() }.invokeOnCompletion {
                        showCollectionSheet = false
                    }
                },
                onSave = { count, spotId ->
                    scope.launch {
                        appContainer.collectionRepository.insertCollection(
                            com.example.data.local.CollectionEntryEntity(
                                containerCount = count,
                                timestamp = System.currentTimeMillis(),
                                estimatedValueCents = count * com.example.util.Constants.DEPOSIT_VALUE_CENTS,
                                collectionSpotId = spotId ?: uiState.selectedSpot?.id,
                                note = null,
                                latitude = customPickedPos?.first ?: uiState.selectedSpot?.latitude,
                                longitude = customPickedPos?.second ?: uiState.selectedSpot?.longitude
                            )
                        )
                        customPickedPos = null
                        collectionSheetState.hide()
                        showCollectionSheet = false
                    }
                }
            )
        }
    }
}
