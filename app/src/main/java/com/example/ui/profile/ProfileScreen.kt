package com.example.ui.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ViraApp
import com.example.domain.ContainerBalance
import com.example.ui.components.ViraMetric
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.components.ViraTopBar
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraIconSize
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.auth.AuthState
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
    val authState by appContainer.authRepository.authState.collectAsStateWithLifecycle(AuthState.LocalOnly)
    val lastSyncTime by appContainer.syncManager.lastSyncTime.collectAsStateWithLifecycle(null)

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            showAuthDialog = false
        }
    }

    val memberDateText = remember(userProfile) {
        val timestamp = userProfile?.memberSince ?: System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-PT"))
        "Membro desde ${dateFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }}"
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
                ViraTopBar(title = "Perfil", subtitle = "Preferências e conta")
                Spacer(modifier = Modifier.height(ViraSpacing.space16))
            }

            // USER AVATAR & IDENTITY
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(LocalViraExtraColors.current.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(ViraIconSize.large),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                    Column {
                        Text(
                            text = userProfile?.name ?: "Utilizador Vira",
                            style = ViraTypography.MetricMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(ViraSpacing.space4))
                        Text(
                            text = memberDateText,
                            style = ViraTypography.BodySecondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // LIFETIME SUMMARY
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

            // CLOUD BACKUP & SYNC
            item {
                ViraSectionHeader(title = "Cópia de Segurança")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    when (val currentAuth = authState) {
                        is AuthState.Authenticated -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(ViraIconSize.medium)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                    Column {
                                        Text(text = "Backup na nuvem ativo", style = ViraTypography.Body)
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
                        else -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(ViraIconSize.medium)
                                    )
                                    Spacer(modifier = Modifier.width(ViraSpacing.space16))
                                    Column {
                                        Text(
                                            text = "Os teus dados estão guardados neste dispositivo.",
                                            style = ViraTypography.Body
                                        )
                                        Text(
                                            text = "Ativa o backup para sincronizar e proteger as tuas recolhas.",
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
                                        Text("Ativar backup na nuvem")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ViraSpacing.space24))
            }

            // SETTINGS & PREFERENCES
            item {
                ViraSectionHeader(title = "Definições")
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileSettingsRow(
                        icon = Icons.Default.Flag,
                        title = "Meta mensal",
                        value = "500 embalagens"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = "Tema visual",
                        value = "Sistema"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.Download,
                        title = "Exportar dados",
                        value = "JSON"
                    )
                    ProfileDivider()
                    ProfileSettingsRow(
                        icon = Icons.Default.Security,
                        title = "Privacidade e dados locais",
                        value = "No dispositivo"
                    )
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
                                text = "Eliminar todos os dados",
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

            // ABOUT VIRA FOOTER
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vira v1.0",
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space4))
                    Text(
                        text = "Aplicação independente e data-focused. Todos os dados são guardados de forma estritamente local.",
                        style = ViraTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Eliminar todos os dados?") },
                text = { Text("Esta ação apagará permanentemente todas as recolhas, devoluções e spots locais.") },
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
    }
}

@Composable
private fun AuthDialog(
    authState: AuthState,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isSignUp) "Criar Conta Vira" else "Iniciar Sessão")
        },
        text = {
            Column {
                if (authState is AuthState.Error) {
                    Text(
                        text = authState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = ViraTypography.Caption
                    )
                    Spacer(modifier = Modifier.height(ViraSpacing.space8))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Palavra-passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                TextButton(
                    onClick = { isSignUp = !isSignUp },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        if (isSignUp) "Já tens conta? Iniciar sessão"
                        else "Não tens conta? Criar nova conta",
                        style = ViraTypography.Caption
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isSignUp) onSignUp(email.trim(), password)
                    else onSignIn(email.trim(), password)
                },
                enabled = email.isNotBlank() && password.length >= 6 && authState !is AuthState.Loading
            ) {
                Text(
                    if (authState is AuthState.Loading) "A processar..."
                    else if (isSignUp) "Criar conta"
                    else "Entrar"
                )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
