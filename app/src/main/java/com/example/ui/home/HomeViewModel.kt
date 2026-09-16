package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.domain.BalanceService
import com.example.domain.ContainerBalance
import com.example.domain.Insight
import com.example.domain.RecommendationEngine
import com.example.repository.CollectionRepository
import com.example.repository.SpotRepository
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.domain.collection.CollectionDraft
import com.example.domain.location.LocationResult
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

data class HomeUiState(
    val isEmptyUser: Boolean = false,
    val todayContainers: Int = 0,
    val todayEstimatedValue: Long = 0,
    val monthContainers: Int = 0,
    val monthEstimatedValue: Long = 0,
    val monthTrendPercentage: Int? = null,
    val monthSparklineData: List<Float> = emptyList(),
    val balance: ContainerBalance = ContainerBalance(0, 0, 0, 0L, 0L, 0L),
    val topInsight: Insight? = null,
    val spots: List<CollectionSpotEntity> = emptyList(),
    val quickCounterValue: Int = 0,
    val selectedSpotId: Int? = null,
    val bestSpot: CollectionSpotEntity? = null,
    val draft: CollectionDraft = CollectionDraft()
)

class HomeViewModel(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService,
    private val spotRepository: SpotRepository
) : ViewModel() {

    private val recommendationEngine = RecommendationEngine()
    private val _quickCounter = MutableStateFlow(0)
    private val _selectedSpotId = MutableStateFlow<Int?>(null)
    private val _draft = MutableStateFlow(CollectionDraft())
    val draft: StateFlow<CollectionDraft> = _draft.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        collectionRepository.getAllCollections(),
        balanceService.observeBalance(),
        spotRepository.getAllSpots(),
        _draft
    ) { collections, balance, spots, currentDraft ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Today Start
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        // Month Start
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        // Previous Month Start & End
        cal.add(Calendar.MONTH, -1)
        val prevMonthStart = cal.timeInMillis

        val todayCollections = collections.filter { it.timestamp >= todayStart }
        val todayContainers = todayCollections.sumOf { it.containerCount }

        val monthCollections = collections.filter { it.timestamp >= monthStart }
        val monthContainers = monthCollections.sumOf { it.containerCount }

        val prevMonthCollections = collections.filter { it.timestamp in prevMonthStart until monthStart }
        val prevMonthContainers = prevMonthCollections.sumOf { it.containerCount }

        val monthTrend: Int? = if (prevMonthContainers > 0 && monthContainers > 0) {
            val delta = ((monthContainers - prevMonthContainers).toFloat() / prevMonthContainers * 100f).roundToInt()
            delta
        } else null

        // Sparkline data (last 7 days activity)
        val sparkline = (6 downTo 0).map { dayOffset ->
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = now
            dayCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dStart = dayCal.timeInMillis
            dayCal.add(Calendar.DAY_OF_YEAR, 1)
            val dEnd = dayCal.timeInMillis

            collections.filter { it.timestamp in dStart until dEnd }
                .sumOf { it.containerCount }
                .toFloat()
        }

        val insights = recommendationEngine.generateInsights(collections)
        val topInsight = insights.firstOrNull()

        val isEmptyUser = collections.isEmpty() && balance.containersCollected == 0
        val bestSpot = spots.maxByOrNull { it.lifetimeContainers } ?: spots.firstOrNull()

        HomeUiState(
            isEmptyUser = isEmptyUser,
            todayContainers = todayContainers,
            todayEstimatedValue = todayContainers * DEPOSIT_VALUE_CENTS,
            monthContainers = monthContainers,
            monthEstimatedValue = monthContainers * DEPOSIT_VALUE_CENTS,
            monthTrendPercentage = monthTrend,
            monthSparklineData = sparkline,
            balance = balance,
            topInsight = topInsight,
            spots = spots,
            quickCounterValue = currentDraft.quantity,
            selectedSpotId = currentDraft.selectedSpotId,
            bestSpot = bestSpot,
            draft = currentDraft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun updateCounter(amount: Int) {
        val newVal = _quickCounter.value + amount
        if (newVal >= 0) {
            _quickCounter.value = newVal
        }
    }

    fun setCounter(value: Int) {
        if (value >= 0) {
            _quickCounter.value = value
        }
    }

    fun setSelectedSpot(spotId: Int?) {
        _selectedSpotId.value = spotId
    }

    fun setDraftQuantity(quantity: Int) {
        if (quantity >= 1) {
            _draft.update { it.copy(quantity = quantity) }
        }
    }

    fun incrementDraftQuantity() {
        _draft.update { it.copy(quantity = it.quantity + 1) }
    }

    fun decrementDraftQuantity() {
        _draft.update { if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else it }
    }

    fun setDraftLocation(latitude: Double, longitude: Double) {
        _draft.update {
            it.copy(
                selectedLatitude = latitude,
                selectedLongitude = longitude,
                isSheetOpen = true
            )
        }
    }

    fun setDraftSpot(spotId: Int?) {
        _draft.update { it.copy(selectedSpotId = spotId) }
    }

    fun setDraftNote(note: String?) {
        _draft.update { it.copy(note = note) }
    }

    fun openDraftSheet() {
        _draft.update { it.copy(isSheetOpen = true) }
    }

    fun closeDraftSheet() {
        _draft.update { it.copy(isSheetOpen = false) }
    }

    fun resetDraft() {
        _draft.value = CollectionDraft()
    }

    fun saveDraftCollection(onSaved: () -> Unit = {}) {
        val currentDraft = _draft.value
        if (currentDraft.quantity <= 0) return
        viewModelScope.launch {
            val entry = CollectionEntryEntity(
                containerCount = currentDraft.quantity,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = currentDraft.quantity * DEPOSIT_VALUE_CENTS,
                collectionSpotId = currentDraft.selectedSpotId,
                note = currentDraft.note,
                latitude = currentDraft.selectedLatitude,
                longitude = currentDraft.selectedLongitude
            )
            collectionRepository.insertCollection(entry)
            resetDraft()
            _quickCounter.value = 0
            _selectedSpotId.value = null
            onSaved()
        }
    }

    fun saveCollection(count: Int, spotId: Int?, note: String? = null, onSaved: () -> Unit = {}) {
        if (count <= 0) return
        viewModelScope.launch {
            val currentDraft = _draft.value
            val entry = CollectionEntryEntity(
                containerCount = count,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = count * DEPOSIT_VALUE_CENTS,
                collectionSpotId = spotId ?: currentDraft.selectedSpotId,
                note = note ?: currentDraft.note,
                latitude = currentDraft.selectedLatitude,
                longitude = currentDraft.selectedLongitude
            )
            collectionRepository.insertCollection(entry)
            resetDraft()
            _quickCounter.value = 0
            _selectedSpotId.value = null
            onSaved()
        }
    }
}

class HomeViewModelFactory(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService,
    private val spotRepository: SpotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(collectionRepository, balanceService, spotRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
