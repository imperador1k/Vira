package com.example.ui.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ViraApp
import com.example.domain.location.LocationResult
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.ZoomButtons
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapPickerScreen(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    onLocationPicked: (latitude: Double, longitude: Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container
    val locationRepository = appContainer.locationRepository
    val scope = rememberCoroutineScope()

    var isLocating by remember { mutableStateOf(false) }

    val initialTarget = if (initialLatitude != null && initialLongitude != null) {
        Position(longitude = initialLongitude, latitude = initialLatitude)
    } else {
        Position(longitude = -8.0, latitude = 39.5)
    }
    val initialZoom = if (initialLatitude != null && initialLongitude != null) 15.0 else 6.0

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(MapConfig.getStyleUrl()),
        initialCameraPosition = CameraPosition(
            bearing = 0.0,
            target = initialTarget,
            tilt = 0.0,
            zoom = initialZoom
        )
    )

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    fun requestGpsCenter() {
        if (permissionsState.allPermissionsGranted || permissionsState.permissions.any { it.status.isGranted }) {
            scope.launch {
                isLocating = true
                when (val result = locationRepository.getCurrentLocation()) {
                    is LocationResult.Success -> {
                        isLocating = false
                        mapState.animateCameraPosition(
                            CameraPosition(
                                target = Position(longitude = result.longitude, latitude = result.latitude),
                                zoom = 15.5
                            )
                        )
                    }
                    is LocationResult.ApproximateOnly -> {
                        isLocating = false
                        mapState.animateCameraPosition(
                            CameraPosition(
                                target = Position(longitude = result.longitude, latitude = result.latitude),
                                zoom = 14.0
                            )
                        )
                    }
                    else -> {
                        isLocating = false
                    }
                }
            }
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Map Canvas with ZoomButtons in MapOverlayScope
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = mapState,
            overlay = {
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
            }
        )

        // 2. Fixed Center Crosshair
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00D1B2).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Mira de seleção",
                    tint = Color(0xFF00D1B2),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 3. Top Header Bar with Close Button and Title
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space8),
            shape = RoundedCornerShape(ViraRadius.large),
            color = LocalViraExtraColors.current.surfaceElevated.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Escolher no mapa", style = ViraTypography.MetricMedium)
                    Text(
                        text = "Move o mapa para apontar o local",
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                }
            }
        }

        // 4. GPS MyLocation Button
        Surface(
            onClick = { requestGpsCenter() },
            shape = CircleShape,
            color = LocalViraExtraColors.current.surfaceElevated,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ViraSpacing.space16, bottom = 180.dp)
                .size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar na minha localização",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 6. Bottom Confirmation Sheet
        val currentTarget = mapState.cameraPosition.target
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(ViraSpacing.space16),
            shape = RoundedCornerShape(ViraRadius.large),
            color = LocalViraExtraColors.current.surfaceElevated,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(ViraSpacing.space16)) {
                Text(
                    text = "Coordenadas apontadas",
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "%.5f, %.5f", currentTarget.latitude, currentTarget.longitude),
                    style = ViraTypography.MetricMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", style = ViraTypography.ButtonLabel)
                    }
                    ViraPrimaryButton(
                        text = "Selecionar este local",
                        onClick = {
                            onLocationPicked(currentTarget.latitude, currentTarget.longitude)
                        },
                        modifier = Modifier.weight(2f)
                    )
                }
            }
        }
    }
}
