package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.domain.Insight
import com.example.domain.RecommendationEngine
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToRedemption: () -> Unit = {}) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container
    
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(appContainer.collectionRepository, appContainer.balanceService)
    )
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val recommendationEngine = remember { RecommendationEngine() }
    val collections by appContainer.collectionRepository.getAllCollections().collectAsStateWithLifecycle(emptyList())
    val insights = remember(collections) { recommendationEngine.generateInsights(collections) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBottomSheet = true },
                icon = { Icon(Icons.Default.Add, "Registar recolha") },
                text = { Text("Registar", style = MaterialTheme.typography.labelLarge) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vira",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { /* TODO Navigate Profile */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil", modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            item {
                Text(
                    text = "Hoje",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${uiState.todayContainers}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 88.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "embalagens",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
                
                if (uiState.todayEstimatedValue > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+${FormatUtils.formatCurrency(uiState.todayEstimatedValue)} potencial",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(56.dp))
            }

            item {
                Text(
                    text = "Balanço",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Containers
                Row(modifier = Modifier.fillMaxWidth()) {
                    BalanceStat(
                        modifier = Modifier.weight(1f),
                        label = "Recolhidas", 
                        value = uiState.balance.containersCollected.toString()
                    )
                    BalanceStat(
                        modifier = Modifier.weight(1f),
                        label = "Devolvidas", 
                        value = uiState.balance.containersAccepted.toString()
                    )
                    BalanceStat(
                        modifier = Modifier.weight(1f),
                        label = "Em curso", 
                        value = uiState.balance.containersRemainingToReturn.toString(),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))
                
                // Financials
                Row(modifier = Modifier.fillMaxWidth()) {
                    FinancialStat(
                        modifier = Modifier.weight(1f),
                        label = "Potencial", 
                        value = FormatUtils.formatCurrency(uiState.balance.potentialValueCents)
                    )
                    FinancialStat(
                        modifier = Modifier.weight(1f),
                        label = "Recuperado", 
                        value = FormatUtils.formatCurrency(uiState.balance.recoveredValueCents)
                    )
                    FinancialStat(
                        modifier = Modifier.weight(1f),
                        label = "A recuperar", 
                        value = FormatUtils.formatCurrency(uiState.balance.remainingPotentialValueCents),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedButton(
                    onClick = onNavigateToRedemption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Registar devolução", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(56.dp))
            }
            
            if (insights.isNotEmpty()) {
                item {
                    Text(
                        text = "Padrões",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    insights.forEach { insight ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    when (insight) {
                                        is Insight.BestDay -> {
                                            Text("Melhor dia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${insight.dayOfWeek} (${String.format("%.1f", insight.averageContainers)}/dia)",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        is Insight.Consistency -> {
                                            Text("Consistência", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${insight.weeksInARow} semanas seguidas",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        is Insight.Milestone -> {
                                            Text("Marco alcançado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                insight.message,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registar recolha", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { viewModel.updateCounter(-1) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remover")
                    }
                    Text(
                        text = "${uiState.quickCounterValue}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    IconButton(
                        onClick = { viewModel.updateCounter(1) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { viewModel.updateCounter(1) }) { Text("+1") }
                    OutlinedButton(onClick = { viewModel.updateCounter(5) }) { Text("+5") }
                    OutlinedButton(onClick = { viewModel.updateCounter(10) }) { Text("+10") }
                    OutlinedButton(onClick = { viewModel.updateCounter(25) }) { Text("+25") }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val potentialValue = uiState.quickCounterValue * 10L
                Text(
                    text = "Valor estimado: ${FormatUtils.formatCurrency(potentialValue)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = {
                        viewModel.saveQuickCollection()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.quickCounterValue > 0,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar recolha", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BalanceStat(modifier: Modifier = Modifier, label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun FinancialStat(modifier: Modifier = Modifier, label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
