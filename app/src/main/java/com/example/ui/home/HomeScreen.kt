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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ViraApp
import com.example.data.auth.AuthState
import com.example.data.preferences.UserProfileData
import com.example.domain.Insight
import java.io.File
import com.example.ui.components.AuthDialog
import com.example.ui.components.ViraEmptyState
import com.example.ui.components.ViraInsightCard
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.components.ViraSparkline
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraIconSize
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
            spotRepository = appContainer.spotRepository,
            locationRepository = appContainer.locationRepository
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by appContainer.authRepository.authState.collectAsStateWithLifecycle(AuthState.LocalOnly)
    val sessionCount by appContainer.userPreferencesRepository.sessionCount.collectAsStateWithLifecycle(1)
    val lastReminderTime by appContainer.userPreferencesRepository.lastReminderTime.collectAsStateWithLifecycle(0L)
    val isReminderDismissedForever by appContainer.userPreferencesRepository.isReminderDismissedForever.collectAsStateWithLifecycle(false)
    val profilePrefs by appContainer.userPreferencesRepository.profileData.collectAsStateWithLifecycle(
        initialValue = UserProfileData()
    )

    val avatarBitmap = remember(profilePrefs.avatarFilePath) {
        profilePrefs.avatarFilePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    var dismissedThisSession by rememberSaveable { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            showAuthDialog = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val isSheetVisible = showBottomSheet || uiState.draft.isSheetOpen

    val isEligibleForReminder = remember(
        authState,
        sessionCount,
        uiState.balance.containersCollected,
        lastReminderTime,
        isReminderDismissedForever,
        dismissedThisSession
    ) {
        if (authState is AuthState.Authenticated) return@remember false
        if (isReminderDismissedForever) return@remember false
        if (dismissedThisSession) return@remember false

        val fourteenDaysMillis = 14L * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        if (now - lastReminderTime < fourteenDaysMillis) return@remember false

        sessionCount >= 3 || uiState.balance.containersCollected >= 20
    }

    val shouldShowReminder = isEligibleForReminder && !isSheetVisible

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
                ViraTopBar(
                    title = "Vira",
                    avatarBitmap = avatarBitmap,
                    onProfileClick = onNavigateToProfile
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space32))

                if (shouldShowReminder) {
                    AccountReminderCard(
                        onCreateAccount = { showAuthDialog = true },
                        onRemindLater = {
                            dismissedThisSession = true
                            scope.launch { appContainer.userPreferencesRepository.snoozeAccountReminder() }
                        },
                        onDismissForever = {
                            dismissedThisSession = true
                            scope.launch { appContainer.userPreferencesRepository.dismissAccountReminderForever() }
                        }
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                }

                ViraEmptyState(
                    title = "Começa a recuperar valor.",
                    subtitle = "Cada embalagem retornável equivale a 0,10 €. Regista as tuas recolhas e acompanha o teu saldo, devoluções e pontos mais produtivos.",
                    ctaText = "+ Registar primeira recolha",
                    onCtaClick = { showBottomSheet = true },
                    footnote = "Modo local ativo • 100% privado no teu telemóvel"
                )
            }
        } else {
            // POPULATED HOME - Open Activity Dashboard with distinct Vira visual hierarchy
            val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 5..11 -> "Bom dia 👋"
                    in 12..19 -> "Boa tarde 👋"
                    else -> "Boa noite 👋"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = ViraSpacing.space24),
                contentPadding = PaddingValues(bottom = ViraSpacing.space48)
            ) {
                item {
                    ViraTopBar(
                        title = "Vira",
                        subtitle = greeting,
                        avatarBitmap = avatarBitmap,
                        onProfileClick = onNavigateToProfile
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                }

                // NON-INTRUSIVE ACCOUNT REMINDER
                if (shouldShowReminder) {
                    item {
                        AccountReminderCard(
                            onCreateAccount = { showAuthDialog = true },
                            onRemindLater = {
                                dismissedThisSession = true
                                scope.launch { appContainer.userPreferencesRepository.snoozeAccountReminder() }
                            },
                            onDismissForever = {
                                dismissedThisSession = true
                                scope.launch { appContainer.userPreferencesRepository.dismissAccountReminderForever() }
                            }
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    }
                }

                // 1. HERO RECOLHA SECTION (Dominant collection amount in refined card)
                item {
                    ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECOLHA DE HOJE",
                                    style = ViraTypography.Eyebrow,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (uiState.todayEstimatedValue > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(ViraRadius.small)
                                ) {
                                    Text(
                                        text = "+ ${FormatUtils.formatCurrency(uiState.todayEstimatedValue)}",
                                        style = ViraTypography.Caption.copy(
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(ViraSpacing.space12))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${uiState.todayContainers}",
                                style = ViraTypography.DisplayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (uiState.todayContainers == 1) "embalagem hoje" else "embalagens hoje",
                                style = ViraTypography.BodySecondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(ViraSpacing.space16))

                        // HERO PRIMARY ACTION: "+ Registar recolha"
                        com.example.ui.components.ViraHeroButton(
                            text = "+ Registar recolha",
                            onClick = { showBottomSheet = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
                }

                // 2. CONTEXTUAL PROGRESS: Por devolver
                val collected = uiState.balance.containersCollected
                val accepted = uiState.balance.containersAccepted
                val returnRate = if (collected > 0) (accepted.toFloat() / collected).coerceIn(0f, 1f) else 0f
                val remaining = uiState.balance.containersRemainingToReturn

                if (collected > 0) {
                    item {
                        ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "POR DEVOLVER",
                                    style = ViraTypography.Eyebrow,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(uiState.balance.remainingPotentialValueCents),
                                    style = ViraTypography.ButtonLabel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(ViraSpacing.space8))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$remaining",
                                    style = ViraTypography.DisplayMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                Text(
                                    text = if (remaining == 1) "embalagem restante" else "embalagens restantes",
                                    style = ViraTypography.BodySecondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            com.example.ui.components.ViraProgressBar(
                                progress = returnRate,
                                height = 6.dp
                            )
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$accepted devolvidas de $collected (${(returnRate * 100).toInt()}%)",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Registar devolução →",
                                    style = ViraTypography.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onNavigateToRedemption)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(ViraSpacing.space24))
                    }
                }

                // 3. BENTO GRID OF CONTEXTUAL CARDS:
                // - Esta semana / Este mês
                // - Meta atual
                item {
                    val trendText = uiState.monthTrendPercentage?.let { if (it >= 0) "+$it%" else "$it%" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space12)
                    ) {
                        com.example.ui.components.ViraStatCard(
                            eyebrow = "ESTE MÊS",
                            value = "${uiState.monthContainers}",
                            subtitle = "${FormatUtils.formatCurrency(uiState.monthEstimatedValue)} potencial",
                            badgeText = trendText,
                            modifier = Modifier.weight(1f)
                        )
                        com.example.ui.components.ViraStatCard(
                            eyebrow = "TAXA DEVOLUÇÃO",
                            value = "${(returnRate * 100).toInt()}%",
                            subtitle = if (remaining > 0) "$remaining por devolver" else "Tudo em dia",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space12))
                }

                // SPOTS & INSIGHT ROW
                item {
                    val best = uiState.bestSpot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space12)
                    ) {
                        if (best != null) {
                            com.example.ui.components.ViraStatCard(
                                eyebrow = "MELHOR SPOT",
                                value = best.name,
                                subtitle = "${best.lifetimeContainers} recolhidas",
                                icon = Icons.Default.Place,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToSpot(best.id) }
                            )
                        } else {
                            com.example.ui.components.ViraStatCard(
                                eyebrow = "SPOTS",
                                value = "Explorar",
                                subtitle = "Descobre no mapa",
                                icon = Icons.Default.Place,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToMap
                            )
                        }

                        // Insight or Map StatCard
                        if (uiState.topInsight != null) {
                            val insightTitle = when (uiState.topInsight) {
                                is Insight.BestDay -> "Melhor Dia"
                                is Insight.Consistency -> "Consistência"
                                is Insight.Milestone -> "Marco"
                                else -> "Atividade"
                            }
                            val insightDesc = when (val ins = uiState.topInsight) {
                                is Insight.BestDay -> "${ins.dayOfWeek}"
                                is Insight.Consistency -> "${ins.weeksInARow} semanas"
                                is Insight.Milestone -> ins.message
                                else -> "Em curso"
                            }
                            com.example.ui.components.ViraStatCard(
                                eyebrow = insightTitle.uppercase(),
                                value = insightDesc,
                                subtitle = "Com base no histórico",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            com.example.ui.components.ViraStatCard(
                                eyebrow = "MAPA",
                                value = "Pontos Vira",
                                subtitle = "Devolução & spots",
                                icon = Icons.Default.Place,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToMap
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(ViraSpacing.space24))
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
                initialNote = draft.note,
                pickedCoordinate = if (draft.hasLocation) Pair(draft.selectedLatitude!!, draft.selectedLongitude!!) else null,
                onQuantityChange = { newCount ->
                    viewModel.setDraftQuantity(newCount)
                },
                onNoteChange = { newNote ->
                    viewModel.setDraftNote(newNote)
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showBottomSheet = false
                        viewModel.closeDraftSheet()
                    }
                },
                onSave = { count, spotId, note ->
                    viewModel.setDraftQuantity(count)
                    viewModel.setDraftSpot(spotId)
                    viewModel.setDraftNote(note)
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
                },
                onUseCurrentLocation = {
                    viewModel.useCurrentLocationForDraft()
                }
            )
        }

        if (showAuthDialog) {
            AuthDialog(
                authState = authState,
                onDismiss = { showAuthDialog = false },
                onSignIn = { email, pass ->
                    scope.launch {
                        appContainer.authRepository.signIn(email, pass)
                    }
                },
                onSignUp = { email, pass ->
                    scope.launch {
                        appContainer.authRepository.signUp(email, pass)
                    }
                },
                onResetPassword = { email ->
                    scope.launch {
                        appContainer.authRepository.sendPasswordResetEmail(email)
                    }
                }
            )
        }
    }
}

@Composable
private fun AccountReminderCard(
    onCreateAccount: () -> Unit,
    onRemindLater: () -> Unit,
    onDismissForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    ViraSurfaceCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ViraIconSize.medium)
            )
            Spacer(modifier = Modifier.width(ViraSpacing.space12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Protege o teu histórico",
                    style = ViraTypography.ButtonLabel,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                Text(
                    text = "Cria uma conta para fazer backup das tuas recolhas, recuperar os teus dados noutro telemóvel e personalizar o teu perfil.",
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(ViraSpacing.space12))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismissForever) {
                Text(
                    text = "Não voltar a mostrar",
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(ViraSpacing.space4))
            TextButton(onClick = onRemindLater) {
                Text(
                    text = "Agora não",
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(ViraSpacing.space4))
            TextButton(onClick = onCreateAccount) {
                Text(
                    text = "Criar conta",
                    style = ViraTypography.ButtonLabel,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

