package com.example.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ViraApp
import com.example.domain.Insight
import com.example.ui.components.ViraEmptyState
import com.example.ui.components.ViraInsightCard
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.components.ViraSparkline
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.FormatUtils
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController? = null,
    onNavigateToRedemption: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToMapPicker: () -> Unit = {},
    onNavigateToSpot: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            collectionRepository = appContainer.collectionRepository,
            balanceService = appContainer.balanceService,
            spotRepository = appContainer.spotRepository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Observe coordinate returned from MapPickerScreen via SavedStateHandle
    val currentBackStack = navController?.currentBackStackEntry
    LaunchedEffect(currentBackStack) {
        currentBackStack?.savedStateHandle?.getStateFlow<Double?>("picked_latitude", null)
            ?.collect { lat ->
                val lng = currentBackStack.savedStateHandle.get<Double>("picked_longitude")
                if (lat != null && lng != null) {
                    viewModel.setDraftLocation(lat, lng)
                    showBottomSheet = true
                    currentBackStack.savedStateHandle.remove<Double>("picked_latitude")
                    currentBackStack.savedStateHandle.remove<Double>("picked_longitude")
                }
            }
    }

    Scaffold { innerPadding ->
        if (uiState.isEmptyUser) {
            // EMPTY HOME - Dedicated geospatial aesthetic, zero recycling cliches
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = ViraSpacing.space24)
            ) {
                ViraTopBar(title = "Vira", onProfileClick = onNavigateToProfile)
                Spacer(modifier = Modifier.height(ViraSpacing.space32))
                ViraEmptyState(
                    title = "A tua primeira recolha\ncomeça aqui.",
                    subtitle = "Regista as embalagens que encontrares.\nA Vira acompanha o teu progresso, valor a recuperar e os teus locais mais produtivos.",
                    ctaText = "+ Registar primeira recolha",
                    onCtaClick = { showBottomSheet = true },
                    footnote = "1 embalagem elegível = 0,10 €"
                )
            }
        } else {
            // POPULATED HOME - Open Activity Dashboard (No boxy card-on-card stacking)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = ViraSpacing.space24),
                contentPadding = PaddingValues(bottom = ViraSpacing.space48)
            ) {
                item {
                    ViraTopBar(title = "Vira", onProfileClick = onNavigateToProfile)
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                }

                // 1. HOJE (Hero activity focus)
                item {
                    Text(
                        text = "HOJE",
                        style = ViraTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.todayContainers}",
                            style = ViraTypography.HeroNumber.copy(fontSize = 54.sp, lineHeight = 58.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(ViraSpacing.space8))
                        Text(
                            text = if (uiState.todayContainers == 1) "embalagem" else "embalagens",
                            style = ViraTypography.BodySecondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                    Text(
                        text = "${FormatUtils.formatCurrency(uiState.todayEstimatedValue)} potencial",
                        style = ViraTypography.Body,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    ViraPrimaryButton(
                        text = "+ Registar recolha",
                        onClick = { showBottomSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                }

                // 2. ESTE MÊS (Trends & micro-sparkline)
                item {
                    val trendText = uiState.monthTrendPercentage?.let { if (it >= 0) "+$it%" else "$it%" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTE MÊS",
                            style = ViraTypography.SectionTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (trendText != null) {
                            Text(
                                text = trendText,
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space12))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "${uiState.monthContainers}",
                                style = ViraTypography.MetricMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.monthContainers == 1) "embalagem" else "embalagens",
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = FormatUtils.formatCurrency(uiState.monthEstimatedValue),
                                style = ViraTypography.MetricMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "potencial",
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.monthSparklineData.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(ViraSpacing.space16))
                        ViraSparkline(
                            data = uiState.monthSparklineData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                }

                // 3. POR DEVOLVER (Activity progress & quick recovery action)
                item {
                    val collected = uiState.balance.containersCollected
                    val accepted = uiState.balance.containersAccepted
                    val returnRate = if (collected > 0) (accepted.toFloat() / collected).coerceIn(0f, 1f) else 0f
                    val animatedRate by animateFloatAsState(targetValue = returnRate, label = "returnRateAnim")
                    val remaining = uiState.balance.containersRemainingToReturn

                    Text(
                        text = "POR DEVOLVER",
                        style = ViraTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$remaining",
                            style = ViraTypography.HeroNumber.copy(fontSize = 40.sp, lineHeight = 44.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(ViraSpacing.space8))
                        Text(
                            text = if (remaining == 1) "embalagem" else "embalagens",
                            style = ViraTypography.BodySecondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = "${FormatUtils.formatCurrency(uiState.balance.remainingPotentialValueCents)} por recuperar",
                        style = ViraTypography.BodySecondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$collected recolhidas · $accepted devolvidas",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space8))
                    // Thin progress line (4.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(LocalViraExtraColors.current.surfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedRate)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onNavigateToRedemption)
                            .padding(vertical = ViraSpacing.space4),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registar devolução →",
                            style = ViraTypography.ButtonLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                }

                // 4. PARA TI (RecommendationEngine insight)
                uiState.topInsight?.let { insight ->
                    item {
                        Text(
                            text = "PARA TI",
                            style = ViraTypography.SectionTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space8))
                        when (insight) {
                            is Insight.BestDay -> {
                                ViraInsightCard(
                                    title = "Dia mais produtivo",
                                    description = "Com base no teu histórico, costumas recolher em média ${String.format(Locale.getDefault(), "%.1f", insight.averageContainers)} embalagens às ${insight.dayOfWeek.lowercase()}s."
                                )
                            }
                            is Insight.Consistency -> {
                                ViraInsightCard(
                                    title = "Consistência de recolha",
                                    description = "Nas últimas ${insight.weeksInARow} semanas mantiveste registos contínuos de recolha."
                                )
                            }
                            is Insight.Milestone -> {
                                ViraInsightCard(
                                    title = "Marco alcançado",
                                    description = insight.message
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    }
                }

                // 5. PERTO DE TI (Compact geospatial preview)
                item {
                    Text(
                        text = "PERTO DE TI",
                        style = ViraTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space12))
                    val best = uiState.bestSpot
                    if (best != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ViraRadius.medium))
                                .background(LocalViraExtraColors.current.surfaceElevated)
                                .clickable { onNavigateToSpot(best.id) }
                                .padding(ViraSpacing.space16),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space12))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = best.name,
                                    style = ViraTypography.ButtonLabel,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                Text(
                                    text = "${best.lifetimeContainers} recolhidas · ${String.format(Locale.getDefault(), "%.1f", best.averageContainersPerVisit)} / visita",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ViraRadius.medium))
                                .background(LocalViraExtraColors.current.surfaceElevated)
                                .clickable { onNavigateToMap() }
                                .padding(ViraSpacing.space16),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space12))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Explorar mapa",
                                    style = ViraTypography.ButtonLabel,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                Text(
                                    text = "Descobre pontos de devolução e cria spots de recolha",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // COLLECTION MODAL BOTTOM SHEET
        val isSheetVisible = showBottomSheet || uiState.draft.isSheetOpen
        if (isSheetVisible) {
            val draft = uiState.draft
            CollectionBottomSheet(
                sheetState = sheetState,
                spots = uiState.spots,
                initialCount = draft.quantity,
                initialSpotId = draft.selectedSpotId,
                pickedCoordinate = if (draft.hasLocation) Pair(draft.selectedLatitude!!, draft.selectedLongitude!!) else null,
                onQuantityChange = { newCount ->
                    viewModel.setDraftQuantity(newCount)
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                        viewModel.closeDraftSheet()
                    }
                },
                onSave = { count, spotId ->
                    viewModel.setDraftQuantity(count)
                    viewModel.setDraftSpot(spotId)
                    viewModel.saveDraftCollection {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            viewModel.closeDraftSheet()
                        }
                    }
                },
                onPickOnMap = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                        viewModel.closeDraftSheet()
                        onNavigateToMapPicker()
                    }
                }
            )
        }
    }
}
