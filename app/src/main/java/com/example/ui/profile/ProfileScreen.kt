package com.example.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ViraApp
import com.example.data.auth.AuthState
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserProfileData
import com.example.domain.ContainerBalance
import com.example.ui.components.AuthDialog
import com.example.ui.components.ViraMetric
import com.example.ui.components.ViraSectionHeader
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

    val hasFineLocation = remember { appContainer.locationRepository.hasFineLocationPermission() }
    val hasCoarseLocation = remember { appContainer.locationRepository.hasCoarseLocationPermission() }
    val isLocationPermissionGranted = hasFineLocation || hasCoarseLocation
    val isLocationServicesEnabled = remember { appContainer.locationRepository.isLocationEnabled() }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                appContainer.userPreferencesRepository.saveAvatarFromUri(uri)
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

            // 1. PROFILE (PHOTO, NAME, USERNAME, CITY & EDIT)
            item {
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with overlay badge
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
                            // Subtle camera badge overlay
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
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 2. LIFETIME SUMMARY STATS
            item {
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TOTAL VITALÍCIO",
                        style = ViraTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space16))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ViraMetric(
                            label = "Recolhidas",
                            value = "${balance.containersCollected}",
                            modifier = Modifier.weight(1f)
                        )
                        ViraMetric(
                            label = "Recuperado",
                            value = FormatUtils.formatCurrency(balance.recoveredValueCents),
                            modifier = Modifier.weight(1f),
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 3. ACCOUNT (LOCAL MODE VS AUTHENTICATED & BENEFITS)
            item {
                ViraSectionHeader(title = "Conta")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    when (val currentAuth = authState) {
                        is AuthState.Authenticated -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(ViraIconSize.medium)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Backup na nuvem ativo (Beta)",
                                            style = ViraTypography.Body
                                        )
                                        Text(
                                            text = currentAuth.email,
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val syncText = if (lastSyncTime != null) {
                                            "Última sincronização: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime!!))}"
                                        } else {
                                            "Sincronização pendente"
                                        }
                                        Text(
                                            text = syncText,
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space12))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { scope.launch(Dispatchers.IO) { appContainer.syncManager.syncAll() } }
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(ViraIconSize.small))
                                        Spacer(modifier = Modifier.width(ViraSpacing.space4))
                                        Text("Sincronizar agora")
                                    }
                                    Spacer(modifier = Modifier.width(ViraSpacing.space8))
                                    TextButton(
                                        onClick = { scope.launch { appContainer.authRepository.signOut() } }
                                    ) {
                                        Text("Terminar sessão", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(ViraIconSize.medium)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                    Column {
                                        Text(
                                            text = "Modo Local (Sem conta)",
                                            style = ViraTypography.Body
                                        )
                                        Text(
                                            text = "Os teus dados estão guardados em segurança no teu telemóvel.",
                                            style = ViraTypography.Caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(ViraSpacing.space12))

                                // Clear explanation of account benefits (Section 9)
                                Surface(
                                    color = LocalViraExtraColors.current.surfaceInteractive,
                                    shape = RoundedCornerShape(ViraRadius.small),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(ViraSpacing.space12)) {
                                        Text(
                                            text = "Benefícios de associar uma conta:",
                                            style = ViraTypography.ButtonLabel,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                                        Text(
                                            text = "• Sem conta: recolhas, histórico, mapa, progresso, spots pessoais e utilização 100% offline.\n• Com conta: backup na nuvem, recuperação de dados se trocares de telemóvel e sincronização multi-dispositivo.",
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
                                        Text("Criar conta / Iniciar sessão")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 4. APPEARANCE (TEMA VISUAL)
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

            // 5. DATA & BACKUP (STATUS)
            item {
                ViraSectionHeader(title = "Dados e Cópia de Segurança")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileSettingsRow(
                        icon = Icons.Default.Storage,
                        title = "Base de dados local",
                        value = "SQLite Room (No telemóvel)"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = if (authState is AuthState.Authenticated) Icons.Default.CloudDone else Icons.Default.Cloud,
                        title = "Backup na nuvem",
                        value = if (authState is AuthState.Authenticated) "Ativo (Supabase)" else "Desativado"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.Sync,
                        title = "Sincronização",
                        value = when {
                            authState !is AuthState.Authenticated -> "Apenas local"
                            lastSyncTime != null -> "Atualizado"
                            else -> "Pendente"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 6. LOCATION SERVICES & PERMISSIONS STATUS
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

            // 7. ABOUT VIRA & PRIVACY
            item {
                ViraSectionHeader(title = "Sobre")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileSettingsRow(
                        icon = Icons.Default.Info,
                        title = "Versão da aplicação",
                        value = "1.0 (Beta Pessoal)"
                    )
                    ProfileDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ViraSpacing.space8)
                    ) {
                        Text(
                            text = "Privacidade e dados",
                            style = ViraTypography.Body
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Text(
                            text = "As coordenadas de GPS são utilizadas para situar as tuas recolhas no mapa local. Nenhum dado pessoal ou de localização é vendido ou partilhado.",
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Tecnologia de mapas",
                        value = "OpenFreeMap · OpenStreetMap"
                    )
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // 8. DANGER ZONE
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
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ViraIconSize.small)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space32))
            }

            // FOOTER
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vira — Aplicação Pessoal",
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                    Text(
                        text = "Desenvolvida com foco em privacidade e rapidez.",
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
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
                    Column {
                        Text("Podes escolher uma nova foto da tua galeria ou remover a atual.")
                    }
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
                }
            )
        }

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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalViraExtraColors.current.divider)
    )
}

