package com.example.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.RedemptionEntryEntity
import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.repository.SpotRepository
import com.example.ui.components.ViraPeriodSelector
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraTimelineEntry
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class HistoryItemModel(val timestamp: Long) {
    data class CollectionItem(
        val entry: CollectionEntryEntity,
        val spotName: String? = null
    ) : HistoryItemModel(entry.timestamp)

    data class RedemptionItem(
        val entry: RedemptionEntryEntity
    ) : HistoryItemModel(entry.timestamp)
}

class HistoryViewModel(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository,
    private val spotRepository: SpotRepository
) : ViewModel() {

    private val _filterIndex = MutableStateFlow(0) // 0: Todas, 1: Recolhas, 2: Devoluções

    val filterIndex: StateFlow<Int> = _filterIndex

    val historyGrouped: StateFlow<Map<String, List<HistoryItemModel>>> = combine(
        collectionRepository.getAllCollections(),
        redemptionRepository.getAllRedemptions(),
        spotRepository.getAllSpots(),
        _filterIndex
    ) { collections, redemptions, spots, filter ->
        val spotMap = spots.associateBy { it.id }
        val items = mutableListOf<HistoryItemModel>()

        if (filter == 0 || filter == 1) {
            items.addAll(collections.map {
                HistoryItemModel.CollectionItem(it, it.collectionSpotId?.let { id -> spotMap[id]?.name })
            })
        }
        if (filter == 0 || filter == 2) {
            items.addAll(redemptions.map {
                HistoryItemModel.RedemptionItem(it)
            })
        }

        val sorted = items.sortedByDescending { it.timestamp }

        // Group by day header (Hoje, Ontem, or dd MMMM)
        val cal = Calendar.getInstance()
        val nowCal = Calendar.getInstance()
        val locale = Locale.forLanguageTag("pt-PT")
        val dateFormat = SimpleDateFormat("d 'de' MMMM", locale)

        sorted.groupBy { item ->
            cal.timeInMillis = item.timestamp
            val isToday = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

            when {
                isToday -> "HOJE"
                isYesterday -> "ONTEM"
                else -> dateFormat.format(Date(item.timestamp)).uppercase(locale)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun setFilter(index: Int) {
        _filterIndex.value = index
    }

    fun deleteCollection(id: Int) {
        viewModelScope.launch {
            collectionRepository.deleteCollectionById(id)
        }
    }

    fun deleteRedemption(id: Int) {
        viewModelScope.launch {
            redemptionRepository.deleteRedemptionById(id)
        }
    }
}

class HistoryViewModelFactory(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository,
    private val spotRepository: SpotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(collectionRepository, redemptionRepository, spotRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(
            collectionRepository = appContainer.collectionRepository,
            redemptionRepository = appContainer.redemptionRepository,
            spotRepository = appContainer.spotRepository
        )
    )

    val groupedItems by viewModel.historyGrouped.collectAsStateWithLifecycle()
    val filterIndex by viewModel.filterIndex.collectAsStateWithLifecycle()
    val filterLabels = listOf("Todas", "Recolhas", "Devoluções")

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var itemToDelete by remember { mutableStateOf<HistoryItemModel?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = ViraSpacing.space24)
        ) {
            ViraTopBar(title = "Histórico", subtitle = "Timeline de atividade")
            Spacer(modifier = Modifier.height(ViraSpacing.space16))

            ViraPeriodSelector(
                periods = filterLabels,
                selectedIndex = filterIndex,
                onSelectPeriod = { viewModel.setFilter(it) }
            )

            Spacer(modifier = Modifier.height(ViraSpacing.space16))

            if (groupedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = ViraSpacing.space48),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem atividade registada neste filtro.",
                        style = ViraTypography.BodySecondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ViraSpacing.space48)
                ) {
                    groupedItems.forEach { (dayHeader, items) ->
                        item {
                            Spacer(modifier = Modifier.height(ViraSpacing.space16))
                            ViraSectionHeader(title = dayHeader)
                            Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        }

                        items(items.size) { index ->
                            val item = items[index]
                            when (item) {
                                is HistoryItemModel.CollectionItem -> {
                                    val entry = item.entry
                                    val spotLabel = when {
                                        item.spotName != null -> item.spotName
                                        entry.latitude != null && entry.longitude != null -> "Localização guardada"
                                        else -> "Sem localização associada"
                                    }
                                    val time = timeFormat.format(Date(entry.timestamp))
                                    val containerLabel = if (entry.containerCount == 1) "1 embalagem" else "${entry.containerCount} embalagens"
                                    ViraTimelineEntry(
                                        isCollection = true,
                                        category = "RECOLHA",
                                        locationName = spotLabel,
                                        metricText = containerLabel,
                                        time = time,
                                        valueText = "+${FormatUtils.formatCurrency(entry.estimatedValueCents)}",
                                        isLast = index == items.size - 1,
                                        onClick = { itemToDelete = item }
                                    )
                                }
                                is HistoryItemModel.RedemptionItem -> {
                                    val entry = item.entry
                                    val time = timeFormat.format(Date(entry.timestamp))
                                    val noteLabel = entry.note?.takeIf { it.isNotBlank() } ?: "Ponto de devolução"
                                    val acceptedText = if (entry.acceptedContainers == 1) "1 aceite" else "${entry.acceptedContainers} aceites"
                                    val metricLabel = if (entry.rejectedContainers > 0) "$acceptedText · ${entry.rejectedContainers} rejeitadas" else acceptedText
                                    ViraTimelineEntry(
                                        isCollection = false,
                                        category = "DEVOLUÇÃO",
                                        locationName = noteLabel,
                                        metricText = metricLabel,
                                        time = time,
                                        valueText = "+${FormatUtils.formatCurrency(entry.actualRecoveredCents)}",
                                        isLast = index == items.size - 1,
                                        onClick = { itemToDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CONFIRM DELETE DIALOG
        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Eliminar registo") },
                text = { Text("Desejas remover este registo? Os teus balanços e estatísticas serão recalculados automaticamente.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            when (item) {
                                is HistoryItemModel.CollectionItem -> viewModel.deleteCollection(item.entry.id)
                                is HistoryItemModel.RedemptionItem -> viewModel.deleteRedemption(item.entry.id)
                            }
                            itemToDelete = null
                        }
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
