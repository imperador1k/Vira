package com.example.ui.redemption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.RedemptionEntryEntity
import com.example.repository.RedemptionRepository
import kotlinx.coroutines.launch

class RedemptionViewModel(
    private val redemptionRepository: RedemptionRepository
) : ViewModel() {

    fun saveRedemption(
        presented: Int,
        accepted: Int,
        rejected: Int,
        returnPointId: Int? = null,
        note: String? = null
    ) {
        if (presented <= 0 || accepted < 0 || rejected < 0) return
        if (accepted + rejected > presented) return
        
        viewModelScope.launch {
            val entry = RedemptionEntryEntity(
                presentedContainers = presented,
                acceptedContainers = accepted,
                rejectedContainers = rejected,
                actualRecoveredCents = accepted * 10L, // 10 cents per accepted container
                timestamp = System.currentTimeMillis(),
                returnPointId = returnPointId,
                note = note
            )
            redemptionRepository.insertRedemption(entry)
        }
    }
}

class RedemptionViewModelFactory(
    private val redemptionRepository: RedemptionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RedemptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RedemptionViewModel(redemptionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
