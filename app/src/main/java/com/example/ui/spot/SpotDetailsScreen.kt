package com.example.ui.spot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.repository.SpotRepository
import com.example.ui.components.ViraIconButton
import com.example.ui.components.ViraMetric
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.components.ViraSecondaryButton
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.home.CollectionBottomSheet
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpotViewModel(
    private val spotId: Int,
    private val spotRepository: SpotRepository
) : ViewModel() {

    val spot: StateFlow<CollectionSpotEntity?> = spotRepository.getSpotById(spotId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteSpot() {
        viewModelScope.launch {
            spotRepository.deleteSpotById(spotId)
        }
    }
}

class SpotViewModelFactory(
    private val spotId: Int,
    private val spotRepository: SpotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpotViewModel(spotId, spotRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailsScreen(
    spotId: Int,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: SpotViewModel = viewModel(
        factory = SpotViewModelFactory(spotId, appContainer.spotRepository)
    )

    val spot by viewModel.spot.collectAsStateWithLifecycle()
    var showCollectionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        if (spot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val s = spot!!
            val locale = Locale.forLanguageTag("pt-PT")
            val dateFormat = SimpleDateFormat("dd MMM yyyy", locale)
            val lifetimeValue = s.lifetimeContainers * DEPOSIT_VALUE_CENTS

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = ViraSpacing.space24)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Bar
                Spacer(modifier = Modifier.height(ViraSpacing.space16))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViraIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        onClick = onNavigateUp
                    )
                    Text(text = s.name, style = ViraTypography.PageTitle)
                    ViraIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Remover Spot",
                        onClick = {
                            viewModel.deleteSpot()
                            onNavigateUp()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(ViraSpacing.space24))

                // LIFETIME CONTAINERS HERO CARD
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "HISTÓRICO DE RECOLHAS",
                        style = ViraTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space8))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${s.lifetimeContainers}",
                            style = ViraTypography.HeroNumber,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(horizontal = ViraSpacing.space4))
                        Text(
                            text = "embalagens",
                            style = ViraTypography.BodySecondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = ViraSpacing.space16)
                        )
                    }
                    Text(
                        text = "${FormatUtils.formatCurrency(lifetimeValue)} potencial gerado",
                        style = ViraTypography.BodySecondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    ViraPrimaryButton(
                        text = "+ Registar recolha aqui",
                        onClick = { showCollectionSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(ViraSpacing.space16))

                // METRICS CARD
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ViraMetric(
                            label = "Visitas",
                            value = "${s.totalVisits}",
                            modifier = Modifier.weight(1f)
                        )
                        ViraMetric(
                            label = "Média / visita",
                            value = String.format("%.1f", s.averageContainersPerVisit),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    ViraMetric(
                        label = "Última visita",
                        value = s.lastVisitedAt?.let { dateFormat.format(Date(it)) } ?: "Sem registo"
                    )
                    if (!s.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(ViraSpacing.space16))
                        Text(
                            text = "ENDEREÇO",
                            style = ViraTypography.HeroLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Text(
                            text = s.address,
                            style = ViraTypography.Body,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ViraSpacing.space32))
            }

            if (showCollectionSheet) {
                CollectionBottomSheet(
                    sheetState = sheetState,
                    spots = listOf(s),
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showCollectionSheet = false
                        }
                    },
                    onSave = { count, _, note ->
                        scope.launch {
                            appContainer.collectionRepository.insertCollection(
                                CollectionEntryEntity(
                                    containerCount = count,
                                    timestamp = System.currentTimeMillis(),
                                    estimatedValueCents = count * DEPOSIT_VALUE_CENTS,
                                    collectionSpotId = s.id,
                                    note = note?.takeIf { it.isNotBlank() },
                                    latitude = s.latitude,
                                    longitude = s.longitude
                                )
                            )
                            sheetState.hide()
                            showCollectionSheet = false
                        }
                    }
                )
            }
        }
    }
}
