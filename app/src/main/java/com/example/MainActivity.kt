package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.navigation.HistoryRoute
import com.example.navigation.HomeRoute
import com.example.navigation.MapRoute
import com.example.navigation.ProfileRoute
import com.example.navigation.ProgressRoute
import com.example.navigation.RedemptionRoute
import com.example.navigation.TopLevelDestination
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
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
        it.hasRoute(MapRoute::class) || 
        it.hasRoute(HistoryRoute::class) || 
        it.hasRoute(ProfileRoute::class) 
    } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.values().forEach { destination ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(HomeRoute) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.titleTextId) },
                            label = { Text(destination.titleTextId) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> { HomeScreen(onNavigateToRedemption = { navController.navigate(RedemptionRoute) }) }
            composable<ProgressRoute> { ProgressScreen() }
            composable<MapRoute> { MapScreen() }
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
