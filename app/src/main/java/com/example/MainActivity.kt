package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import android.util.Log
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.navigation.HistoryRoute
import com.example.navigation.HomeRoute
import com.example.navigation.MapRootRoute
import com.example.navigation.MapPickerRoute
import com.example.navigation.ProfileRoute
import com.example.navigation.ProgressRoute
import com.example.navigation.RedemptionRoute
import com.example.navigation.TopLevelDestination
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.map.MapPickerScreen
import com.example.ui.map.MapScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.progress.ProgressScreen
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.redemption.RedemptionScreen
import com.example.ui.spot.SpotDetailsScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppThemeMode
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.ViraTypography
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = (applicationContext as ViraApp).container
            val themeMode by appContainer.themePreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)

            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                ViraAppScreen()
            }
        }
    }
}

@Composable
fun ViraSplashScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "VIRA",
                style = ViraTypography.DisplayMedium.copy(
                    fontSize = 32.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Economia circular inteligente",
                style = ViraTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
fun ViraAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var pendingMapFocus by remember { mutableStateOf<Triple<Double, Double, Int?>?>(null) }
    var isAppReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(900)
        isAppReady = true
    }

    if (!isAppReady) {
        ViraSplashScreen()
        return
    }

    val showBottomBar = currentDestination?.hierarchy?.any { 
        it.hasRoute(HomeRoute::class) || 
        it.hasRoute(ProgressRoute::class) || 
        it.hasRoute(MapRootRoute::class) || 
        it.hasRoute(HistoryRoute::class) || 
        it.hasRoute(ProfileRoute::class) 
    } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                ViraBottomNav(
                    currentDestination = currentDestination,
                    onNavigateTo = { destination ->
                        if (destination == TopLevelDestination.HOME) {
                            navController.navigate(HomeRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    navController = navController,
                    onNavigateToRedemption = { navController.navigate(RedemptionRoute) },
                    onNavigateToProfile = {
                        navController.navigate(ProfileRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMap = { navController.navigate(MapRootRoute) },
                    onNavigateToMapPicker = { navController.navigate(MapPickerRoute) },
                    onNavigateToSpot = { spotId -> navController.navigate(com.example.navigation.SpotRoute(spotId)) }
                )
            }
            composable<ProgressRoute> {
                ProgressScreen(onNavigateToSpot = { spotId ->
                    navController.navigate(com.example.navigation.SpotRoute(spotId))
                })
            }
            composable<MapRootRoute> {
                MapScreen(
                    onNavigateToSpot = { spotId -> 
                        navController.navigate(com.example.navigation.SpotRoute(spotId)) 
                    },
                    onNavigateToHistory = {
                        navController.navigate(HistoryRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    focusLatitude = pendingMapFocus?.first,
                    focusLongitude = pendingMapFocus?.second,
                    focusCollectionId = pendingMapFocus?.third,
                    onClearFocus = { pendingMapFocus = null }
                ) 
            }
            composable<MapPickerRoute> {
                MapPickerScreen(
                    onLocationPicked = { lat, lng ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_latitude", lat)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_longitude", lng)
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onNavigateToMap = { lat, lng, colId ->
                        pendingMapFocus = Triple(lat, lng, colId)
                        navController.navigate(MapRootRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable<ProfileRoute> { ProfileScreen() }
            composable<RedemptionRoute> { RedemptionScreen(onNavigateUp = { navController.navigateUp() }) }
            composable<com.example.navigation.SpotRoute> { backStackEntry -> 
                val spotRoute: com.example.navigation.SpotRoute = backStackEntry.toRoute()
                SpotDetailsScreen(spotId = spotRoute.spotId, onNavigateUp = { navController.navigateUp() })
            }
        }
    }
}

@Composable
private fun ViraBottomNav(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigateTo: (TopLevelDestination) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = com.example.ui.theme.LocalViraExtraColors.current.cardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = com.example.ui.theme.LocalViraExtraColors.current.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.hasRoute(destination.route::class)
                } == true

                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    label = "navTabColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateTo(destination)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) com.example.ui.theme.LocalViraExtraColors.current.cyanMuted.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.titleTextId,
                            tint = animatedColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = destination.titleTextId,
                        style = com.example.ui.theme.ViraTypography.Caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = animatedColor
                    )
                }
            }
        }
    }
}
