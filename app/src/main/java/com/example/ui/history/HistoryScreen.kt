package com.example.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.home.CollectionBottomSheet
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
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

    fun updateCollection(id: Int, newCount: Int, newNote: String?) {
        viewModelScope.launch {
            collectionRepository.updateCollection(id, newCount, newNote)
        }
    }

    fun deleteRedemption(id: Int) {
        viewModelScope.launch {
            redemptionRepository.deleteRedemptionById(id)
        }
    }

    val allSpots: StateFlow<List<CollectionSpotEntity>> = spotRepository.getAllSpots().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertCollection(count: Int, spotId: Int?, note: String?, latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            collectionRepository.insertCollection(
                CollectionEntryEntity(
                    containerCount = count,
                    timestamp = System.currentTimeMillis(),
                    estimatedValueCents = count * DEPOSIT_VALUE_CENTS,
                    collectionSpotId = spotId,
                    note = note?.takeIf { it.isNotBlank() },
                    latitude = latitude,
                    longitude = longitude
                )
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToMap: ((Double, Double, Int) -> Unit)? = null
) {
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
    val spots by viewModel.allSpots.collectAsStateWithLifecycle()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var selectedCollectionItem by remember { mutableStateOf<HistoryItemModel.CollectionItem?>(null) }
    var itemToRecordAgain by remember { mutableStateOf<HistoryItemModel.CollectionItem?>(null) }
    var itemToEdit by remember { mutableStateOf<HistoryItemModel.CollectionItem?>(null) }
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
                val emptyTitle = when (filterIndex) {
                    1 -> "Sem recolhas registadas"
                    2 -> "Sem devoluções registadas"
                    else -> "Histórico vazio"
                }
                val emptySubtitle = when (filterIndex) {
                    1 -> "As tuas recolhas de embalagens vão aparecer aqui organizadas por data."
                    2 -> "As devoluções efetuadas nos pontos de retoma vão aparecer aqui."
                    else -> "Regista a tua primeira recolha para começares a acompanhar o teu histórico."
                }
                com.example.ui.components.ViraEmptyState(
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = ViraSpacing.space48)
                )
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
                                        onClick = { selectedCollectionItem = item }
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

        // COLLECTION DETAIL BOTTOM SHEET
        selectedCollectionItem?.let { item ->
            val entry = item.entry
            val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { selectedCollectionItem = null },
                sheetState = detailSheetState,
                containerColor = LocalViraExtraColors.current.surfaceElevated,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ViraSpacing.space24)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(ViraRadius.small),
                            color = Color(0xFF0F2E28)
                        ) {
                            Text(
                                text = "RECOLHA",
                                style = ViraTypography.Caption,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { selectedCollectionItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space16))

                    // Quantity & Value
                    val countText = if (entry.containerCount == 1) "1 embalagem" else "${entry.containerCount} embalagens"
                    val valueText = "+${FormatUtils.formatCurrency(entry.estimatedValueCents)}"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = countText, style = ViraTypography.MetricLarge)
                        Text(text = valueText, style = ViraTypography.MetricMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space8))

                    // Date and Time
                    val locale = Locale.forLanguageTag("pt-PT")
                    val fullDateFormat = remember { SimpleDateFormat("d 'de' MMMM yyyy, HH:mm", locale) }
                    Text(
                        text = fullDateFormat.format(Date(entry.timestamp)),
                        style = ViraTypography.BodySecondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(ViraSpacing.space16))

                    // Location info
                    val spotLabel = when {
                        item.spotName != null -> item.spotName
                        entry.latitude != null && entry.longitude != null -> "Localização guardada"
                        else -> "Sem localização associada"
                    }

                    Surface(
                        shape = RoundedCornerShape(ViraRadius.medium),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(ViraSpacing.space12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space8))
                            Column {
                                Text(text = spotLabel, style = ViraTypography.ButtonLabel)
                                if (entry.latitude != null && entry.longitude != null) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.4f", entry.latitude)}, ${String.format(Locale.US, "%.4f", entry.longitude)}",
                                        style = ViraTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Note info
                    if (!entry.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(ViraSpacing.space12))
                        Surface(
                            shape = RoundedCornerShape(ViraRadius.medium),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(ViraSpacing.space12)) {
                                Text(text = "Nota", style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = entry.note, style = ViraTypography.Body)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space24))

                    // Primary Action: Registar aqui novamente
                    ViraPrimaryButton(
                        text = "+ Registar aqui novamente",
                        onClick = {
                            val target = item
                            selectedCollectionItem = null
                            itemToRecordAgain = target
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Secondary Actions: Ver no mapa & Como chegar
                    if (entry.latitude != null && entry.longitude != null) {
                        Spacer(modifier = Modifier.height(ViraSpacing.space8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)
                        ) {
                            if (onNavigateToMap != null) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(ViraRadius.medium))
                                        .clickable {
                                            val lat = entry.latitude
                                            val lng = entry.longitude
                                            val id = entry.id
                                            selectedCollectionItem = null
                                            onNavigateToMap.invoke(lat, lng, id)
                                        },
                                    shape = RoundedCornerShape(ViraRadius.medium),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = ViraSpacing.space12),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ver no mapa",
                                            style = ViraTypography.ButtonLabel,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(ViraRadius.medium))
                                    .clickable {
                                        val gmmIntentUri = Uri.parse("geo:${entry.latitude},${entry.longitude}?q=${entry.latitude},${entry.longitude}(${Uri.encode("Recolha")})")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${entry.latitude},${entry.longitude}"))
                                            context.startActivity(browserIntent)
                                        }
                                    },
                                shape = RoundedCornerShape(ViraRadius.medium),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = ViraSpacing.space12),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Directions,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Como chegar",
                                        style = ViraTypography.ButtonLabel,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space12))

                    // Contextual Actions row: Editar & Eliminar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val target = item
                                selectedCollectionItem = null
                                itemToEdit = target
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                        }

                        TextButton(
                            onClick = {
                                val target = item
                                selectedCollectionItem = null
                                itemToDelete = target
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                }
            }
        }

        // RECORD AGAIN BOTTOM SHEET
        itemToRecordAgain?.let { item ->
            val entry = item.entry
            val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            CollectionBottomSheet(
                sheetState = recordSheetState,
                spots = spots,
                initialSpotId = entry.collectionSpotId,
                pickedCoordinate = if (entry.latitude != null && entry.longitude != null) Pair(entry.latitude, entry.longitude) else null,
                onDismiss = { itemToRecordAgain = null },
                onSave = { count, spotId, note ->
                    viewModel.insertCollection(
                        count = count,
                        spotId = spotId,
                        note = note,
                        latitude = entry.latitude,
                        longitude = entry.longitude
                    )
                    itemToRecordAgain = null
                }
            )
        }

        // EDIT COLLECTION DIALOG
        itemToEdit?.let { item ->
            val entry = item.entry
            var editCount by remember(entry.id) { mutableIntStateOf(entry.containerCount) }
            var editNote by remember(entry.id) { mutableStateOf(entry.note ?: "") }

            AlertDialog(
                onDismissRequest = { itemToEdit = null },
                title = { Text("Editar recolha", style = ViraTypography.MetricMedium) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Quantidade de embalagens", style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(ViraSpacing.space8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (editCount > 1) editCount-- },
                                enabled = editCount > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                            }
                            Text(
                                text = "$editCount",
                                style = ViraTypography.MetricLarge,
                                modifier = Modifier.padding(horizontal = ViraSpacing.space16)
                            )
                            IconButton(onClick = { editCount++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Aumentar")
                            }
                        }
                        Text(
                            text = "Valor estimado: ${FormatUtils.formatCurrency(editCount * DEPOSIT_VALUE_CENTS)}",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space16))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text("Nota (opcional)") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateCollection(entry.id, editCount, editNote.takeIf { it.isNotBlank() })
                            itemToEdit = null
                        }
                    ) {
                        Text("Guardar", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToEdit = null }) {
                        Text("Cancelar", style = ViraTypography.ButtonLabel)
                    }
                }
            )
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
