package com.example.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CollectionEntryEntity
import com.example.domain.BalanceService
import com.example.domain.ContainerBalance
import com.example.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val balance: ContainerBalance = ContainerBalance(0, 0, 0, 0L, 0L, 0L),
    val totalCollections: Int = 0,
    val averagePerCollection: Double = 0.0,
    val activeDays: Int = 0,
    val returnRate: Double = 0.0
)

class ProgressViewModel(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        collectionRepository.getAllCollections(),
        balanceService.observeBalance()
    ) { collections, balance ->
        val totalCollections = collections.size
        val avg = if (totalCollections > 0) balance.containersCollected.toDouble() / totalCollections else 0.0
        
        // Active days
        val activeDays = collections.map { 
            // Simple truncation to days
            it.timestamp / (1000 * 60 * 60 * 24)
        }.distinct().size
        
        val returnRate = if (balance.containersCollected > 0) {
            (balance.containersAccepted.toDouble() / balance.containersCollected) * 100
        } else {
            0.0
        }

        ProgressUiState(
            balance = balance,
            totalCollections = totalCollections,
            averagePerCollection = avg,
            activeDays = activeDays,
            returnRate = returnRate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState()
    )
}

class ProgressViewModelFactory(
    private val collectionRepository: CollectionRepository,
    private val balanceService: BalanceService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgressViewModel(collectionRepository, balanceService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
