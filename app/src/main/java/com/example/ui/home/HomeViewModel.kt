package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CollectionEntryEntity
import com.example.domain.BalanceService
import com.example.domain.ContainerBalance
import com.example.repository.CollectionRepository
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val todayContainers: Int = 0,
    val todayEstimatedValue: Long = 0,
    val balance: ContainerBalance = ContainerBalance(0, 0, 0, 0L, 0L, 0L),
    val quickCounterValue: Int = 0
)

class HomeViewModel(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService
) : ViewModel() {

    private val _quickCounter = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        collectionRepository.getAllCollections(),
        balanceService.observeBalance(),
        _quickCounter
    ) { collections, balance, quickCounter ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        
        val todayCollections = collections.filter { it.timestamp >= todayStart }
        val todayContainers = todayCollections.sumOf { it.containerCount }
        
        HomeUiState(
            todayContainers = todayContainers,
            todayEstimatedValue = todayContainers * DEPOSIT_VALUE_CENTS,
            balance = balance,
            quickCounterValue = quickCounter
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

    fun saveQuickCollection() {
        val count = _quickCounter.value
        if (count <= 0) return
        viewModelScope.launch {
            val entry = CollectionEntryEntity(
                containerCount = count,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = count * DEPOSIT_VALUE_CENTS,
                collectionSpotId = null,
                note = null,
                latitude = null,
                longitude = null
            )
            collectionRepository.insertCollection(entry)
            _quickCounter.value = 0
        }
    }
}

class HomeViewModelFactory(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(collectionRepository, balanceService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
