package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val icon: ImageVector,
    val titleTextId: String,
    val route: Any
) {
    HOME(
        icon = Icons.Default.Home,
        titleTextId = "Início",
        route = HomeRoute
    ),
    MAP(
        icon = Icons.Default.Map,
        titleTextId = "Mapa",
        route = MapRootRoute
    ),
    PROGRESS(
        icon = Icons.Default.BarChart,
        titleTextId = "Progresso",
        route = ProgressRoute
    ),
    HISTORY(
        icon = Icons.Default.History,
        titleTextId = "Histórico",
        route = HistoryRoute
    ),
    PROFILE(
        icon = Icons.Default.Person,
        titleTextId = "Perfil",
        route = ProfileRoute
    )
}
