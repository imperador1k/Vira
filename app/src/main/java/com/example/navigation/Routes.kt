package com.example.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object ProgressRoute

@Serializable
object MapRoute

@Serializable
object HistoryRoute

@Serializable
object ProfileRoute

@Serializable
object RedemptionRoute

@Serializable
data class SpotRoute(val spotId: Int)
