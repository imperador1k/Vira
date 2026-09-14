package com.example.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.RedemptionEntryEntity
import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class HistoryItemModel(val timestamp: Long) {
    data class CollectionItem(val entry: CollectionEntryEntity) : HistoryItemModel(entry.timestamp)
    data class RedemptionItem(val entry: RedemptionEntryEntity) : HistoryItemModel(entry.timestamp)
}

class HistoryViewModel(
    private val collectionRepository: CollectionRepository,
    private val redemptionRepository: RedemptionRepository
) : ViewModel() {
    
    val historyItems: StateFlow<List<HistoryItemModel>> = combine(
        collectionRepository.getAllCollections(),
        redemptionRepository.getAllRedemptions()
    ) { collections, redemptions ->
        val items = mutableListOf<HistoryItemModel>()
        items.addAll(collections.map { HistoryItemModel.CollectionItem(it) })
        items.addAll(redemptions.map { HistoryItemModel.RedemptionItem(it) })
        items.sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
    private val redemptionRepository: RedemptionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(collectionRepository, redemptionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container
    
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(appContainer.collectionRepository, appContainer.redemptionRepository)
    )
    
    val items by viewModel.historyItems.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Histórico",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    text = "Ainda não registaste nenhuma atividade.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                items(items) { item ->
                    when (item) {
                        is HistoryItemModel.CollectionItem -> {
                            CollectionHistoryItem(
                                entry = item.entry,
                                onDelete = { viewModel.deleteCollection(item.entry.id) }
                            )
                        }
                        is HistoryItemModel.RedemptionItem -> {
                            RedemptionHistoryItem(
                                entry = item.entry,
                                onDelete = { viewModel.deleteRedemption(item.entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionHistoryItem(entry: CollectionEntryEntity, onDelete: () -> Unit) {
    val locale = Locale.forLanguageTag("pt-PT")
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", locale)
    val dateString = dateFormat.format(Date(entry.timestamp))
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text("Recolha", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.containerCount} embalagens",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FormatUtils.formatCurrency(entry.estimatedValueCents)} potencial",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) {
                Text("Apagar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun RedemptionHistoryItem(entry: RedemptionEntryEntity, onDelete: () -> Unit) {
    val locale = Locale.forLanguageTag("pt-PT")
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", locale)
    val dateString = dateFormat.format(Date(entry.timestamp))
    val recovered = entry.acceptedContainers * 10L // 10 cents
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text("Devolução", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.acceptedContainers} aceites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.rejectedContainers > 0) {
                    Text(
                        text = "${entry.rejectedContainers} rejeitadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FormatUtils.formatCurrency(recovered)} recuperados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) {
                Text("Apagar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
