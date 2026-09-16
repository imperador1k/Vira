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
import com.example.ui.redemption.RedemptionScreen
import com.example.ui.spot.SpotDetailsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ViraAppScreen()
            }
        }
    }
}

@Composable
fun ViraAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                NavigationBar(
                    containerColor = com.example.ui.theme.LocalViraExtraColors.current.surfaceElevated,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                Log.d("ViraNav", "Click tab: ${destination.name}, before: ${currentDestination?.route}")
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                Log.d("ViraNav", "After click tab: ${destination.name}, current: ${navController.currentBackStackEntry?.destination?.route}")
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.titleTextId) },
                            label = { Text(destination.titleTextId, style = com.example.ui.theme.ViraTypography.Caption) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
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
                    onNavigateToProfile = { navController.navigate(ProfileRoute) },
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
                    }
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
            composable<HistoryRoute> { HistoryScreen() }
            composable<ProfileRoute> { ProfileScreen() }
            composable<RedemptionRoute> { RedemptionScreen(onNavigateUp = { navController.navigateUp() }) }
            composable<com.example.navigation.SpotRoute> { backStackEntry -> 
                val spotRoute: com.example.navigation.SpotRoute = backStackEntry.toRoute()
                SpotDetailsScreen(spotId = spotRoute.spotId, onNavigateUp = { navController.navigateUp() })
            }
        }
    }
}
