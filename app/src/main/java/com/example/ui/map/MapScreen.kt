package com.example.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

// Import MapLibre
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    var isMapFallback by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    val scope = rememberCoroutineScope()
    
    val styleUrl = MapConfig.getStyleUrl()
    val mapState = rememberMapState(baseStyle = BaseStyle.Uri(styleUrl))

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        if (locationPermissionState.status.isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    // Update location and animate camera
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            fetchLocation()
        }
    }

    if (isMapFallback) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Locais", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("O mapa pode não estar disponível no emulador.")
            Button(onClick = { isMapFallback = false }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Voltar ao Mapa")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            MaplibreMap(
                Modifier.fillMaxSize(),
                mapState
            ) {
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { fetchLocation() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, "Localização atual")
                }
                
                FloatingActionButton(
                    onClick = { isMapFallback = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, "Vista de lista")
                }
            }
        }
    }
}
