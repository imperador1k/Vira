package com.example.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CollectionEntryEntity
import com.example.domain.BalanceService
import com.example.domain.ContainerBalance
import com.example.domain.Insight
import com.example.domain.RecommendationEngine
import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.repository.SpotRepository
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.data.local.CollectionSpotEntity
import java.util.Calendar
import kotlin.math.roundToInt

data class ProgressUiState(
    val selectedPeriodIndex: Int = 0,
    val periodContainers: Int = 0,
    val periodTrendPercentage: Int? = null,
    val chartDataPoints: List<Float> = emptyList(),
    val potentialValueCents: Long = 0L,
    val recoveredValueCents: Long = 0L,
    val totalSessions: Int = 0,
    val averagePerSession: Double = 0.0,
    val returnRate: Float = 0f,
    val bestSpotName: String? = null,
    val bestSpot: CollectionSpotEntity? = null,
    val topInsight: Insight? = null
)

class ProgressViewModel(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository,
    private val spotRepository: SpotRepository,
    private val balanceService: BalanceService
) : ViewModel() {

    private val recommendationEngine = RecommendationEngine()
    private val _selectedPeriod = MutableStateFlow(0) // 0: 7d, 1: 30d, 2: Mês, 3: Ano, 4: Sempre

    val uiState: StateFlow<ProgressUiState> = combine(
        collectionRepository.getAllCollections(),
        redemptionRepository.getAllRedemptions(),
        spotRepository.getAllSpots(),
        balanceService.observeBalance(),
        _selectedPeriod
    ) { collections, redemptions, spots, balance, periodIndex ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val periodStartTime = when (periodIndex) {
            0 -> now - (7L * 24 * 60 * 60 * 1000)
            1 -> now - (30L * 24 * 60 * 60 * 1000)
            2 -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            3 -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            else -> 0L // Sempre
        }

        val filteredCollections = if (periodStartTime == 0L) collections else collections.filter { it.timestamp >= periodStartTime }
        val filteredRedemptions = if (periodStartTime == 0L) redemptions else redemptions.filter { it.timestamp >= periodStartTime }

        val periodContainers = filteredCollections.sumOf { it.containerCount }
        val potentialValue = periodContainers * DEPOSIT_VALUE_CENTS
        val recoveredValue = filteredRedemptions.sumOf { it.actualRecoveredCents }
        val sessionsCount = filteredCollections.size
        val avgPerSession = if (sessionsCount > 0) periodContainers.toDouble() / sessionsCount else 0.0

        val returnRate = if (balance.containersCollected > 0) {
            (balance.containersAccepted.toFloat() / balance.containersCollected).coerceIn(0f, 1f)
        } else 0f

        // Best spot in period
        val spotCounts = filteredCollections.filter { it.collectionSpotId != null }
            .groupBy { it.collectionSpotId!! }
            .mapValues { entry -> entry.value.sumOf { it.containerCount } }
        val bestSpotId = spotCounts.maxByOrNull { it.value }?.key
        val bestSpotEntity = spots.firstOrNull { it.id == bestSpotId }
        val bestSpotName = bestSpotEntity?.name

        val periodTrendPercentage = if (periodStartTime > 0L) {
            val periodSpan = now - periodStartTime
            val prevStart = periodStartTime - periodSpan
            val prevContainers = collections.filter { it.timestamp in prevStart until periodStartTime }.sumOf { it.containerCount }
            if (prevContainers > 0) {
                (((periodContainers - prevContainers).toDouble() / prevContainers) * 100).toInt()
            } else null
        } else null

        // Chart data points (split period into 7 buckets)
        val bucketsCount = 7
        val chartData: List<Float> = if (filteredCollections.isEmpty()) {
            emptyList()
        } else {
            val minTime = if (periodStartTime == 0L) (collections.minOfOrNull { it.timestamp } ?: now) else periodStartTime
            val timeRange = (now - minTime).coerceAtLeast(1L)
            val bucketSpan = timeRange / bucketsCount

            (0 until bucketsCount).map { i ->
                val bStart = minTime + (i * bucketSpan)
                val bEnd = bStart + bucketSpan
                filteredCollections.filter { it.timestamp in bStart until bEnd }
                    .sumOf { it.containerCount }
                    .toFloat()
            }
        }

        val insights = recommendationEngine.generateInsights(filteredCollections)

        ProgressUiState(
            selectedPeriodIndex = periodIndex,
            periodContainers = periodContainers,
            periodTrendPercentage = periodTrendPercentage,
            chartDataPoints = chartData,
            potentialValueCents = potentialValue,
            recoveredValueCents = recoveredValue,
            totalSessions = sessionsCount,
            averagePerSession = avgPerSession,
            returnRate = returnRate,
            bestSpotName = bestSpotName,
            bestSpot = bestSpotEntity,
            topInsight = insights.firstOrNull()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState()
    )

    fun selectPeriod(index: Int) {
        _selectedPeriod.value = index
    }
}

class ProgressViewModelFactory(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository,
    private val spotRepository: SpotRepository,
    private val balanceService: BalanceService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(collectionRepository, redemptionRepository, spotRepository, balanceService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
