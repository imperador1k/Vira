package com.example.ui.redemption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp
import com.example.ui.components.ViraIconButton
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.components.ViraSectionHeader
import com.example.ui.components.ViraSurfaceCard
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container

    val viewModel: RedemptionViewModel = viewModel(
        factory = RedemptionViewModelFactory(appContainer.redemptionRepository)
    )

    var presentedText by remember { mutableStateOf("") }
    var acceptedText by remember { mutableStateOf("") }
    var rejectedText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val presented = presentedText.toIntOrNull() ?: 0
    val accepted = acceptedText.toIntOrNull() ?: 0
    val rejected = rejectedText.toIntOrNull() ?: 0

    // Auto-calculate rejected when presented or accepted changes
    LaunchedEffect(presentedText, acceptedText) {
        if (presented > 0 && acceptedText.isNotBlank() && accepted <= presented) {
            rejectedText = (presented - accepted).toString()
        }
    }

    val recoveredCents = accepted * DEPOSIT_VALUE_CENTS
    val isValid = presented > 0 && accepted >= 0 && rejected >= 0 && (accepted + rejected <= presented)

    Scaffold { innerPadding ->
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViraIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    onClick = onNavigateUp
                )
                Spacer(modifier = Modifier.width(ViraSpacing.space16))
                Text(text = "Registar devolução", style = ViraTypography.PageTitle)
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space24))

            // RECOVERED VALUE HERO CARD
            ViraSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VALOR RECUPERADO",
                    style = ViraTypography.SectionTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(ViraSpacing.space8))
                Text(
                    text = FormatUtils.formatCurrency(recoveredCents),
                    style = ViraTypography.HeroNumber,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$accepted aceites · 0,10 € por embalagem",
                    style = ViraTypography.BodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space24))
            ViraSectionHeader(title = "Embalagens")
            Spacer(modifier = Modifier.height(ViraSpacing.space8))

            // Inputs
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LocalViraExtraColors.current.surfaceElevated,
                unfocusedContainerColor = LocalViraExtraColors.current.surfaceElevated,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = LocalViraExtraColors.current.divider
            )

            OutlinedTextField(
                value = presentedText,
                onValueChange = {
                    presentedText = it
                    if (acceptedText.isBlank()) acceptedText = it
                },
                label = { Text("Apresentadas na máquina", style = ViraTypography.Caption) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(ViraRadius.medium),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(ViraSpacing.space12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space12)
            ) {
                OutlinedTextField(
                    value = acceptedText,
                    onValueChange = { acceptedText = it },
                    label = { Text("Aceites", style = ViraTypography.Caption) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(ViraRadius.medium),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = rejectedText,
                    onValueChange = { rejectedText = it },
                    label = { Text("Rejeitadas", style = ViraTypography.Caption) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(ViraRadius.medium),
                    colors = textFieldColors
                )
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space12))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Local ou nota (opcional)", style = ViraTypography.Caption) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = RoundedCornerShape(ViraRadius.medium),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(ViraSpacing.space32))

            ViraPrimaryButton(
                text = if (isValid) "Confirmar devolução (${FormatUtils.formatCurrency(recoveredCents)})" else "Confirmar devolução",
                onClick = {
                    viewModel.saveRedemption(presented, accepted, rejected, null, note.ifBlank { null })
                    onNavigateUp()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid
            )

            Spacer(modifier = Modifier.height(ViraSpacing.space32))
        }
    }
}
