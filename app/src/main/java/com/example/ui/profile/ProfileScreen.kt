package com.example.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ViraApp
import com.example.data.auth.AuthState
import com.example.data.backup.BackupFrequency
import com.example.data.backup.BackupNetworkConstraint
import com.example.data.backup.CloudBackupItem
import com.example.data.backup.ViraBackupPayload
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserProfileData
import com.example.domain.ContainerBalance
import com.example.ui.components.AuthDialog
import com.example.ui.components.ViraMetric
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraStatCard
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraIconSize
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container
    val scope = rememberCoroutineScope()

    val balance by appContainer.balanceService.observeBalance().collectAsStateWithLifecycle(
        initialValue = ContainerBalance(0, 0, 0, 0L, 0L, 0L)
    )
    val userProfile by appContainer.userRepository.getUserProfile().collectAsStateWithLifecycle(null)
    val profilePrefs by appContainer.userPreferencesRepository.profileData.collectAsStateWithLifecycle(
        initialValue = UserProfileData()
    )
    val authState by appContainer.authRepository.authState.collectAsStateWithLifecycle(AuthState.LocalOnly)
    val lastSyncTime by appContainer.syncManager.lastSyncTime.collectAsStateWithLifecycle(null)
    val themeMode by appContainer.themePreferencesRepository.themeMode.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.SYSTEM
    )
    val backupPrefs by appContainer.backupPreferencesRepository.preferences.collectAsStateWithLifecycle(
        initialValue = com.example.data.backup.BackupPreferences()
    )

    val hasFineLocation = remember { appContainer.locationRepository.hasFineLocationPermission() }
    val hasCoarseLocation = remember { appContainer.locationRepository.hasCoarseLocationPermission() }
    val isLocationPermissionGranted = hasFineLocation || hasCoarseLocation
    val isLocationServicesEnabled = remember { appContainer.locationRepository.isLocationEnabled() }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var showCloudBackupsDialog by remember { mutableStateOf(false) }
    var cloudBackupsList by remember { mutableStateOf<List<CloudBackupItem>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    var isBackingUpNow by remember { mutableStateOf(false) }

    var pendingRestorePayload by remember { mutableStateOf<ViraBackupPayload?>(null) }
    var showRestorePreviewDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                appContainer.userPreferencesRepository.saveAvatarFromUri(uri)
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val payload = appContainer.backupManager.createSnapshotPayload()
                val result = appContainer.backupManager.exportToUri(uri, payload)
                if (result.isSuccess) {
                    Toast.makeText(context, "Cópia exportada com sucesso!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Erro ao exportar cópia de segurança.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = appContainer.backupManager.importFromUri(uri)
                result.onSuccess { payload ->
                    pendingRestorePayload = payload
                    showRestorePreviewDialog = true
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Ficheiro inválido", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated || authState is AuthState.AccountMismatch) {
            showAuthDialog = false
        }
    }

    val memberDateText = remember(userProfile) {
        val timestamp = userProfile?.memberSince ?: System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-PT"))
        "Membro desde ${dateFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }}"
    }

    val avatarBitmap = remember(profilePrefs.avatarFilePath) {
        profilePrefs.avatarFilePath?.let { path ->
            try {
                if (File(path).exists()) {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = ViraSpacing.space24),
            contentPadding = PaddingValues(bottom = ViraSpacing.space48)
        ) {
            item {
                ViraTopBar(title = "Perfil", subtitle = "Preferências e definições")
                Spacer(modifier = Modifier.height(ViraSpacing.space16))
            }

            // 1. PERFIL (IDENTITY / LOCAL MODE CARD)
            item {
                when (authState) {
                    is AuthState.Authenticated -> {
                        ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(LocalViraExtraColors.current.surfaceInteractive)
                                        .clickable {
                                            if (profilePrefs.avatarFilePath != null) {
                                                showPhotoOptionsDialog = true
                                            } else {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap,
                                            contentDescription = "Foto de perfil",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Foto de perfil padrão",
                                            modifier = Modifier.size(ViraIconSize.large),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Alterar foto",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(ViraSpacing.space16))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profilePrefs.displayName,
                                        style = ViraTypography.MetricMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (profilePrefs.username.isNotBlank()) {
                                        Text(
                                            text = "@${profilePrefs.username}",
                                            style = ViraTypography.BodySecondary,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (profilePrefs.city.isNotBlank()) {
                                        Text(
                                            text = profilePrefs.city,
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                    Text(
                                        text = memberDateText,
                                        style = ViraTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = LocalViraExtraColors.current.surfaceInteractive,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { showEditProfileDialog = true }
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar perfil",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(ViraIconSize.small)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // LOCAL MODE PROFILE CARD
                        ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(LocalViraExtraColors.current.surfaceInteractive),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar Modo Local",
                                        modifier = Modifier.size(ViraIconSize.large),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo local",
                                        style = ViraTypography.MetricMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                    Text(
                                        text = "Os teus dados estão guardados neste dispositivo.",
                                        style = ViraTypography.Caption,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            Surface(
                                color = LocalViraExtraColors.current.surfaceInteractive,
                                shape = RoundedCornerShape(ViraRadius.small),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Protege o teu histórico, personaliza o teu perfil e recupera os teus dados se mudares de telemóvel.",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(ViraSpacing.space12)
                                )
                            }
                            Spacer(modifier = Modifier.height(ViraSpacing.space12))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAuthDialog = true }) {
                                    Text("Criar conta", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // TOTAL VITALÍCIO
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space12)
                ) {
                    ViraStatCard(
                        eyebrow = "Recolhidas",
                        value = "${balance.containersCollected}",
                        subtitle = "Total vitalício",
                        modifier = Modifier.weight(1f)
                    )
                    ViraStatCard(
                        eyebrow = "Recuperado",
                        value = FormatUtils.formatCurrency(balance.recoveredValueCents),
                        subtitle = "Valor devolvido",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 2. CONTA (AUTHENTICATION & ACCOUNT STATE)
            item {
                ViraSectionHeader(title = "Conta")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    when (val currentAuth = authState) {
                        is AuthState.Authenticated -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ProfileSettingsRow(
                                    icon = Icons.Default.Person,
                                    title = "Conta associada",
                                    value = currentAuth.email
                                )
                                ProfileDivider()
                                ProfileClickableRow(
                                    icon = Icons.Default.Lock,
                                    title = "Alterar palavra-passe",
                                    subtitle = "Receber link no teu email",
                                    onClick = {
                                        scope.launch {
                                            appContainer.authRepository.sendPasswordResetEmail(currentAuth.email)
                                            Toast.makeText(context, "Email de recuperação enviado para ${currentAuth.email}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                                ProfileDivider()
                                ProfileClickableRow(
                                    icon = Icons.AutoMirrored.Filled.Logout,
                                    title = "Terminar sessão",
                                    subtitle = "Regressar ao Modo Local neste telemóvel",
                                    isDestructive = true,
                                    onClick = {
                                        scope.launch {
                                            appContainer.userPreferencesRepository.removeAvatar()
                                            appContainer.userPreferencesRepository.saveProfile("Utilizador Vira", "", "")
                                            appContainer.authRepository.signOut()
                                        }
                                    }
                                )
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(ViraIconSize.medium)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Modo Local Ativo", style = ViraTypography.Body)
                                        Text(
                                            text = "Cria uma conta para proteger o teu progresso na nuvem.",
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space12))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showAuthDialog = true }) {
                                        Text("Entrar ou Criar conta", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 3. APARÊNCIA (THEME)
            item {
                ViraSectionHeader(title = "Aparência")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8)
                    ) {
                        val options = listOf(
                            Triple(AppThemeMode.SYSTEM, "Sistema", Icons.Default.BrightnessAuto),
                            Triple(AppThemeMode.LIGHT, "Claro", Icons.Default.LightMode),
                            Triple(AppThemeMode.DARK, "Escuro", Icons.Default.DarkMode)
                        )
                        options.forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(ViraRadius.medium))
                                    .clickable {
                                        scope.launch {
                                            appContainer.themePreferencesRepository.setThemeMode(mode)
                                        }
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else LocalViraExtraColors.current.surfaceInteractive,
                                shape = RoundedCornerShape(ViraRadius.medium),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = ViraSpacing.space12, horizontal = ViraSpacing.space8),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                    Text(
                                        text = label,
                                        style = ViraTypography.ButtonLabel,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 4. DADOS E BACKUP
            item {
                ViraSectionHeader(title = "Dados e Backup")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    // Continuous Sync Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (authState is AuthState.Authenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(ViraIconSize.medium)
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space16))
                            Column {
                                Text(text = "Sincronização em tempo real", style = ViraTypography.Body)
                                val syncSubtitle = when {
                                    authState !is AuthState.Authenticated -> "Apenas no dispositivo"
                                    lastSyncTime != null -> "Atualizado às ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSyncTime!!))}"
                                    else -> "Sincronização pendente"
                                }
                                Text(text = syncSubtitle, style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (authState is AuthState.Authenticated) {
                            TextButton(
                                onClick = { scope.launch(Dispatchers.IO) { appContainer.syncManager.syncAll() } }
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sincronizar")
                            }
                        }
                    }

                    ProfileDivider()

                    // Automatic Cloud Backup Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Backup automático na nuvem", style = ViraTypography.Body)
                            Text(
                                text = if (authState is AuthState.Authenticated) {
                                    if (backupPrefs.isAutoBackupEnabled) "Ativo · ${if (backupPrefs.frequency == BackupFrequency.DAILY) "Diário" else "Semanal"}"
                                    else "Desativado"
                                } else "Requer conta Vira",
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = backupPrefs.isAutoBackupEnabled && authState is AuthState.Authenticated,
                            onCheckedChange = { isChecked ->
                                if (authState is AuthState.Authenticated) {
                                    scope.launch {
                                        appContainer.backupPreferencesRepository.setAutoBackupEnabled(isChecked)
                                    }
                                } else {
                                    showAuthDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Backup configuration options if enabled
                    if (backupPrefs.isAutoBackupEnabled && authState is AuthState.Authenticated) {
                        ProfileDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = ViraSpacing.space8),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Frequência", style = ViraTypography.Body)
                            Row {
                                TextButton(
                                    onClick = { scope.launch { appContainer.backupPreferencesRepository.setFrequency(BackupFrequency.DAILY) } }
                                ) {
                                    Text(
                                        "Diário",
                                        color = if (backupPrefs.frequency == BackupFrequency.DAILY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { scope.launch { appContainer.backupPreferencesRepository.setFrequency(BackupFrequency.WEEKLY) } }
                                ) {
                                    Text(
                                        "Semanal",
                                        color = if (backupPrefs.frequency == BackupFrequency.WEEKLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        ProfileDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = ViraSpacing.space8),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Ligação de rede", style = ViraTypography.Body)
                            Row {
                                TextButton(
                                    onClick = { scope.launch { appContainer.backupPreferencesRepository.setNetworkConstraint(BackupNetworkConstraint.WIFI_ONLY) } }
                                ) {
                                    Text(
                                        "Apenas Wi-Fi",
                                        color = if (backupPrefs.networkConstraint == BackupNetworkConstraint.WIFI_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { scope.launch { appContainer.backupPreferencesRepository.setNetworkConstraint(BackupNetworkConstraint.ANY_NETWORK) } }
                                ) {
                                    Text(
                                        "Qualquer rede",
                                        color = if (backupPrefs.networkConstraint == BackupNetworkConstraint.ANY_NETWORK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    ProfileDivider()

                    // Last Backup status and Trigger Now
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Último backup na nuvem", style = ViraTypography.Body)
                            val lastBackupText = if (backupPrefs.lastBackupTimestamp > 0) {
                                SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(backupPrefs.lastBackupTimestamp))
                            } else {
                                "Nenhum backup recente"
                            }
                            Text(
                                text = "$lastBackupText (${backupPrefs.lastBackupStatus})",
                                style = ViraTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (authState is AuthState.Authenticated) {
                            if (isBackingUpNow) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            isBackingUpNow = true
                                            val payload = appContainer.backupManager.createSnapshotPayload()
                                            val res = appContainer.backupManager.uploadSnapshot(payload, "MANUAL")
                                            isBackingUpNow = false
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Cópia na nuvem concluída!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Erro ao criar cópia na nuvem.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text("Fazer agora")
                                }
                            }
                        }
                    }

                    // Backup Actions: History, Export, Import (full-width clean rows, avoiding any text wrapping)
                    ProfileClickableRow(
                        icon = Icons.Default.History,
                        title = "Histórico de cópias na nuvem",
                        subtitle = "Consultar e restaurar versões salvas",
                        onClick = {
                            if (authState is AuthState.Authenticated) {
                                showCloudBackupsDialog = true
                                isLoadingBackups = true
                                scope.launch {
                                    val result = appContainer.backupManager.fetchCloudBackups()
                                    isLoadingBackups = false
                                    cloudBackupsList = result.getOrDefault(emptyList())
                                }
                            } else {
                                showAuthDialog = true
                            }
                        }
                    )

                    ProfileDivider()

                    ProfileClickableRow(
                        icon = Icons.Default.FileUpload,
                        title = "Exportar cópia de segurança",
                        subtitle = "Guardar ficheiro .vira neste dispositivo",
                        onClick = {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                            exportBackupLauncher.launch("Vira_Backup_$today.vira")
                        }
                    )

                    ProfileDivider()

                    ProfileClickableRow(
                        icon = Icons.Default.FileDownload,
                        title = "Restaurar cópia de segurança",
                        subtitle = "Importar dados a partir de ficheiro .vira",
                        onClick = {
                            importBackupLauncher.launch(arrayOf("*/*", "application/octet-stream"))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 5. LOCALIZAÇÃO (LOCATION PERMISSION & GPS SERVICE)
            item {
                ViraSectionHeader(title = "Localização")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isLocationPermissionGranted) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = if (isLocationPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(ViraIconSize.medium)
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space16))
                            Column {
                                Text(text = "Permissão da aplicação", style = ViraTypography.Body)
                                Text(
                                    text = if (isLocationPermissionGranted) "Localização precisa autorizada" else "Permissão em falta",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!isLocationPermissionGranted) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Autorizar")
                            }
                        }
                    }
                    ProfileDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isLocationServicesEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                contentDescription = null,
                                tint = if (isLocationServicesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(ViraIconSize.medium)
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space16))
                            Column {
                                Text(text = "Serviços de GPS do Android", style = ViraTypography.Body)
                                Text(
                                    text = if (isLocationServicesEnabled) "Localização do sistema ativada" else "Localização desativada nas definições",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!isLocationServicesEnabled) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Ativar GPS")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 6. PRIVACIDADE
            item {
                ViraSectionHeader(title = "Privacidade")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8)
                    ) {
                        Text(text = "Privacidade dos teus dados", style = ViraTypography.Body)
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Text(
                            text = "• Os locais e coordenadas das tuas recolhas são estritamente privados por padrão.\n• O backup ou sincronização na nuvem não torna os teus dados nem as tuas localizações públicas.\n• Qualquer partilha futura com a comunidade exigirá a tua autorização explícita (opt-in).\n• Nenhum dado pessoal é vendido a terceiros.",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 7. SOBRE
            item {
                ViraSectionHeader(title = "Sobre")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileSettingsRow(
                        icon = Icons.Default.Info,
                        title = "Versão da aplicação",
                        value = "0.2.0-beta"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Tecnologia de mapas",
                        value = "OpenFreeMap · OpenStreetMap"
                    )
                    ProfileDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8)
                    ) {
                        Text(text = "Vira — Aplicação Pessoal", style = ViraTypography.Body)
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Text(
                            text = "Concebida para gestão pessoal de devolução de embalagens com foco em soberania de dados, rapidez e funcionamento offline.",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // DANGER ZONE
            item {
                ViraSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDataDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(ViraIconSize.medium)
                            )
                            Spacer(modifier = Modifier.width(ViraSpacing.space16))
                            Text(
                                text = "Eliminar todos os dados locais",
                                style = ViraTypography.ButtonLabel,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space32))
            }
        }

        // EDIT PROFILE DIALOG
        if (showEditProfileDialog) {
            EditProfileDialog(
                initialDisplayName = profilePrefs.displayName,
                initialUsername = profilePrefs.username,
                initialCity = profilePrefs.city,
                onDismiss = { showEditProfileDialog = false },
                onSave = { newDisplayName, newUsername, newCity ->
                    scope.launch {
                        appContainer.userPreferencesRepository.saveProfile(
                            displayName = newDisplayName,
                            username = newUsername,
                            city = newCity
                        )
                        userProfile?.let { currentEntity ->
                            appContainer.userRepository.saveUserProfile(
                                currentEntity.copy(name = newDisplayName.trim().ifEmpty { "Utilizador Vira" })
                            )
                        }
                        showEditProfileDialog = false
                    }
                }
            )
        }

        // PHOTO OPTIONS DIALOG
        if (showPhotoOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoOptionsDialog = false },
                title = { Text("Foto de Perfil") },
                text = {
                    Text("Podes escolher uma nova foto da tua galeria através do Seletor do Android ou remover a foto atual.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Alterar foto")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showPhotoOptionsDialog = false
                                scope.launch {
                                    appContainer.userPreferencesRepository.removeAvatar()
                                }
                            }
                        ) {
                            Text("Remover foto", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(ViraSpacing.space8))
                        TextButton(onClick = { showPhotoOptionsDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }

        // CLEAR DATA DIALOG
        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Eliminar todos os dados?") },
                text = { Text("Esta ação apagará permanentemente todas as recolhas, devoluções e spots locais guardados neste telemóvel.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                appContainer.userRepository.clearAllUserData()
                            }
                            showClearDataDialog = false
                        }
                    ) {
                        Text("Apagar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // AUTH DIALOG
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

        // CLOUD BACKUPS HISTORY DIALOG
        if (showCloudBackupsDialog) {
            AlertDialog(
                onDismissRequest = { showCloudBackupsDialog = false },
                title = { Text("Cópias de Segurança na Nuvem") },
                text = {
                    if (isLoadingBackups) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (cloudBackupsList.isEmpty()) {
                        Text("Ainda não tens nenhuma cópia de segurança na nuvem.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            items(cloudBackupsList) { item ->
                                Surface(
                                    color = LocalViraExtraColors.current.surfaceInteractive,
                                    shape = RoundedCornerShape(ViraRadius.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = formatIsoDate(item.createdAt),
                                                style = ViraTypography.ButtonLabel
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${item.collectionsCount} recolhas · ${item.spotsCount} locais",
                                                style = ViraTypography.Caption,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            item.deviceModel?.let { model ->
                                                Text(
                                                    text = model,
                                                    style = ViraTypography.Caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        item.payload?.let { payload ->
                                            TextButton(
                                                onClick = {
                                                    pendingRestorePayload = payload
                                                    showCloudBackupsDialog = false
                                                    showRestorePreviewDialog = true
                                                }
                                            ) {
                                                Text("Restaurar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCloudBackupsDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        // RESTORE PREVIEW & DESTRUCTIVE WARNING DIALOG
        if (showRestorePreviewDialog && pendingRestorePayload != null) {
            val payload = pendingRestorePayload!!
            AlertDialog(
                onDismissRequest = { showRestorePreviewDialog = false },
                title = { Text("Restaurar Cópia de Segurança?") },
                text = {
                    Column {
                        Text(
                            text = "ATENÇÃO: Os dados locais atuais serão substituídos pelos dados contidos nesta cópia.",
                            color = MaterialTheme.colorScheme.error,
                            style = ViraTypography.Body
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space12))
                        Surface(
                            color = LocalViraExtraColors.current.surfaceInteractive,
                            shape = RoundedCornerShape(ViraRadius.small),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Resumo da cópia a restaurar:",
                                    style = ViraTypography.ButtonLabel
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Data: ${formatIsoDate(payload.metadata.createdAt)}\n• Recolhas: ${payload.collections.size} (${payload.metadata.totalContainers} embalagens)\n• Locais pessoais: ${payload.spots.size}\n• Devoluções: ${payload.redemptions.size}",
                                    style = ViraTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val result = appContainer.backupManager.restoreSnapshot(payload)
                                showRestorePreviewDialog = false
                                pendingRestorePayload = null
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Dados restaurados com sucesso!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Erro ao restaurar dados.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Confirmar e Substituir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRestorePreviewDialog = false
                            pendingRestorePayload = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // MISMATCH STATE
        val mismatchState = authState as? AuthState.AccountMismatch
        var showDestructiveConfirmation by remember { mutableStateOf(false) }

        if (mismatchState != null) {
            AlertDialog(
                onDismissRequest = { /* Non-dismissible blocking dialog */ },
                title = { Text("Conflito de Conta") },
                text = {
                    Text("Os dados guardados neste dispositivo estão associados a outra conta.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { showDestructiveConfirmation = true }
                    ) {
                        Text(
                            "Usar esta conta e apagar os dados locais",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                appContainer.authRepository.signOut()
                            }
                        }
                    ) {
                        Text("Entrar na conta associada")
                    }
                }
            )

            if (showDestructiveConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDestructiveConfirmation = false },
                    title = { Text("Tem a certeza absoluta?") },
                    text = {
                        Text("Esta ação irá eliminar permanentemente todas as recolhas, devoluções e spots locais guardados neste dispositivo. A conta ${mismatchState.currentEmail} passará a ser a única proprietária.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDestructiveConfirmation = false
                                scope.launch(Dispatchers.IO) {
                                    appContainer.clearPersonalDataAndBind(mismatchState.currentUserId)
                                }
                            }
                        ) {
                            Text("Confirmar e Apagar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDestructiveConfirmation = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

private fun formatIsoDate(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val date = Date.from(instant)
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        iso
    }
}

@Composable
private fun EditProfileDialog(
    initialDisplayName: String,
    initialUsername: String,
    initialCity: String,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, city: String) -> Unit
) {
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var username by remember { mutableStateOf(initialUsername) }
    var city by remember { mutableStateOf(initialCity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nome de apresentação") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nome de utilizador (opcional)") },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(displayName, username, city) },
                enabled = displayName.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ProfileSettingsRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ViraIconSize.medium)
            )
            Spacer(modifier = Modifier.width(ViraSpacing.space16))
            Text(text = title, style = ViraTypography.Body)
        }
        Text(
            text = value,
            style = ViraTypography.BodySecondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ViraRadius.small))
            .clickable(onClick = onClick)
            .padding(vertical = ViraSpacing.space12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ViraIconSize.medium)
            )
            Spacer(modifier = Modifier.width(ViraSpacing.space16))
            Column {
                Text(
                    text = title,
                    style = ViraTypography.Body,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalViraExtraColors.current.divider)
    )
}
