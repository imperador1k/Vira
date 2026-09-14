package com.example.domain

import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.max

data class ContainerBalance(
    val containersCollected: Int,
    val containersAccepted: Int,
    val containersRemainingToReturn: Int,
    val potentialValueCents: Long,
    val recoveredValueCents: Long,
    val remainingPotentialValueCents: Long
)

class BalanceService(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository
) {
    fun observeBalance(): Flow<ContainerBalance> {
        return combine(
            collectionRepository.getAllCollections(),
            redemptionRepository.getAllRedemptions()
        ) { collections, redemptions ->
            val collected = collections.sumOf { it.containerCount }
            val accepted = redemptions.sumOf { it.acceptedContainers }
            val remaining = max(0, collected - accepted) // Never negative

            val potentialValue = collected * DEPOSIT_VALUE_CENTS
            val recoveredValue = accepted * DEPOSIT_VALUE_CENTS
            val remainingPotential = remaining * DEPOSIT_VALUE_CENTS

            ContainerBalance(
                containersCollected = collected,
                containersAccepted = accepted,
                containersRemainingToReturn = remaining,
                potentialValueCents = potentialValue,
                recoveredValueCents = recoveredValue,
                remainingPotentialValueCents = remainingPotential
            )
        }
    }
}
