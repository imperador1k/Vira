package com.example.ui.redemption

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ViraApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ViraApp).container
    
    val viewModel: RedemptionViewModel = viewModel(
        factory = RedemptionViewModelFactory(appContainer.redemptionRepository)
    )

    var presented by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Derived logic for auto-calculating accepted/rejected
    LaunchedEffect(presented, accepted) {
        val p = presented.toIntOrNull() ?: 0
        val a = accepted.toIntOrNull()
        if (p > 0 && a != null && a <= p) {
            rejected = (p - a).toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registar devolução") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Detalhes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = presented,
                onValueChange = { presented = it },
                label = { Text("Embalagens apresentadas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = accepted,
                    onValueChange = { accepted = it },
                    label = { Text("Aceites") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = rejected,
                    onValueChange = { rejected = it },
                    label = { Text("Rejeitadas") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))

            val p = presented.toIntOrNull() ?: 0
            val a = accepted.toIntOrNull() ?: 0
            val r = rejected.toIntOrNull() ?: 0
            val isValid = p > 0 && a >= 0 && r >= 0 && (a + r <= p)

            Button(
                onClick = {
                    viewModel.saveRedemption(p, a, r, null, note.ifBlank { null })
                    onNavigateUp()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirmar devolução", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
