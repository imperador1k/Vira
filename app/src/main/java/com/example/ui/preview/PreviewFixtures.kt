package com.example.ui.preview

import com.example.data.local.CollectionSpotEntity
import com.example.domain.ContainerBalance
import com.example.domain.Insight
import com.example.ui.home.HomeUiState
import com.example.ui.progress.ProgressUiState

object PreviewFixtures {
    val spotParqueMunicipal = CollectionSpotEntity(
        id = 1,
        name = "Parque Municipal",
        latitude = 38.7169,
        longitude = -9.1399,
        address = "Av. Principal",
        createdAt = System.currentTimeMillis(),
        lastVisitedAt = System.currentTimeMillis() - 3 * 86400000L,
        totalVisits = 10,
        lifetimeContainers = 184,
        averageContainersPerVisit = 18.4
    )

    val spotEstacao = CollectionSpotEntity(
        id = 2,
        name = "Estação",
        latitude = 38.7138,
        longitude = -9.1394,
        address = "Largo da Estação",
        createdAt = System.currentTimeMillis(),
        lastVisitedAt = System.currentTimeMillis() - 5 * 86400000L,
        totalVisits = 8,
        lifetimeContainers = 97,
        averageContainersPerVisit = 12.1
    )

    val spots = listOf(spotParqueMunicipal, spotEstacao)

    val populatedBalance = ContainerBalance(
        containersCollected = 387,
        containersAccepted = 250,
        containersRemainingToReturn = 137,
        potentialValueCents = 3870L,
        recoveredValueCents = 2500L,
        remainingPotentialValueCents = 1370L
    )

    val populatedHomeState = HomeUiState(
        isEmptyUser = false,
        todayContainers = 23,
        todayEstimatedValue = 230L,
        monthContainers = 387,
        monthEstimatedValue = 3870L,
        monthTrendPercentage = 18,
        monthSparklineData = listOf(12f, 19f, 15f, 28f, 22f, 31f, 23f),
        balance = populatedBalance,
        topInsight = Insight.Milestone("Incrível! Já recolheste mais de 100 embalagens."),
        spots = spots
    )

    val populatedProgressState = ProgressUiState(
        selectedPeriodIndex = 1,
        periodContainers = 387,
        periodTrendPercentage = 18,
        chartDataPoints = listOf(14f, 22f, 18f, 29f, 35f, 20f, 27f, 31f, 24f, 38f, 42f, 29f, 34f, 23f),
        potentialValueCents = 3870L,
        recoveredValueCents = 2500L,
        totalSessions = 14,
        averagePerSession = 27.6,
        returnRate = 0.65f,
        bestSpotName = "Parque Municipal"
    )
}
