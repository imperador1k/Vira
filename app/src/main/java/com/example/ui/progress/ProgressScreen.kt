package com.example.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.ui.components.ViraEmptyState
import com.example.ui.components.ViraPeriodSelector
import com.example.ui.components.ViraProgressBar
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.components.ViraTimeSeriesChart
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.FormatUtils

@Composable
fun ProgressScreen(
    onNavigateToSpot: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModelFactory(
            collectionRepository = appContainer.collectionRepository,
            redemptionRepository = appContainer.redemptionRepository,
            spotRepository = appContainer.spotRepository,
            balanceService = appContainer.balanceService
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val periodLabels = listOf("7 dias", "30 dias", "Mês", "Ano", "Sempre")

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = ViraSpacing.space24),
            contentPadding = PaddingValues(bottom = ViraSpacing.space48)
        ) {
            item {
                ViraTopBar(title = "Progresso", subtitle = "Análise e tendências")
                Spacer(modifier = Modifier.height(ViraSpacing.space16))
                ViraPeriodSelector(
                    periods = periodLabels,
                    selectedIndex = uiState.selectedPeriodIndex,
                    onSelectPeriod = { viewModel.selectPeriod(it) }
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            if (uiState.periodContainers == 0) {
                item {
                    ViraEmptyState(
                        title = "Sem registos neste período",
                        subtitle = "Regista recolhas de embalagens para acompanhares o teu progresso, valor acumulado e tendências.",
                        ctaText = "Ver todos os períodos",
                        onCtaClick = { viewModel.selectPeriod(4) }
                    )
                }
            } else {
                // HERO NUMBER & TREND
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${uiState.periodContainers}",
                            style = ViraTypography.HeroNumber,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)
                        ) {
                            Text(
                                text = "embalagens",
                                style = ViraTypography.BodySecondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.periodTrendPercentage?.let { trend ->
                                val trendText = if (trend >= 0) "+$trend% vs período anterior" else "$trend% vs período anterior"
                                val trendColor = if (trend >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                Text(
                                    text = trendText,
                                    style = ViraTypography.Caption,
                                    color = trendColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                }

                // TIME-SERIES CHART
                if (uiState.chartDataPoints.size >= 2) {
                    item {
                        ViraTimeSeriesChart(
                            data = uiState.chartDataPoints,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    }
                }

                // OPEN LAYOUT METRICS (No giant enclosing box)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ViraSpacing.space24)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = FormatUtils.formatCurrency(uiState.potentialValueCents),
                                    style = ViraTypography.MetricLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "POTENCIAL",
                                    style = ViraTypography.SectionTitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = FormatUtils.formatCurrency(uiState.recoveredValueCents),
                                    style = ViraTypography.MetricLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "RECUPERADO",
                                    style = ViraTypography.SectionTitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${uiState.totalSessions}",
                                    style = ViraTypography.MetricLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "SESSÕES",
                                    style = ViraTypography.SectionTitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format("%.1f", uiState.averagePerSession),
                                    style = ViraTypography.MetricLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "MÉDIA / SESSÃO",
                                    style = ViraTypography.SectionTitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space32))
                }

                // TAXA DE DEVOLUÇÃO (Thin Progress)
                item {
                    val returnPercent = (uiState.returnRate * 100).toInt()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Taxa de devolução",
                                style = ViraTypography.Body,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$returnPercent%",
                                style = ViraTypography.MetricMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(ViraSpacing.space8))
                        ViraProgressBar(
                            progress = uiState.returnRate,
                            height = 4.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space32))
                }

                // MELHOR SPOT
                uiState.bestSpot?.let { spot ->
                    item {
                        ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MELHOR SPOT",
                                style = ViraTypography.SectionTitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Text(
                                text = spot.name,
                                style = ViraTypography.MetricMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(ViraSpacing.space4))
                            Text(
                                text = "${String.format("%.1f", spot.averageContainersPerVisit)} embalagens / visita · ${spot.lifetimeContainers} recolhidas",
                                style = ViraTypography.BodySecondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            Row(
                                modifier = Modifier
                                    .clickable { onNavigateToSpot(spot.id) }
                                    .padding(vertical = ViraSpacing.space4),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space4)
                            ) {
                                Text(
                                    text = "Ver detalhes do local",
                                    style = ViraTypography.ButtonLabel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
