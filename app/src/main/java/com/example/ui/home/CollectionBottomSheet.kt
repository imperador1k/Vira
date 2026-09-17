package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.data.local.CollectionSpotEntity
import com.example.ui.components.ViraPrimaryButton
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography
import com.example.util.Constants.DEPOSIT_VALUE_CENTS
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBottomSheet(
    sheetState: SheetState,
    spots: List<CollectionSpotEntity>,
    initialCount: Int = 1,
    initialSpotId: Int? = null,
    onQuantityChange: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (count: Int, spotId: Int?) -> Unit,
    onPickOnMap: (() -> Unit)? = null,
    onUseCurrentLocation: (() -> Unit)? = null,
    pickedCoordinate: Pair<Double, Double>? = null
) {
    var count by remember(initialCount) { mutableIntStateOf(initialCount) }
    var selectedSpotId by remember(initialSpotId) { mutableStateOf<Int?>(initialSpotId) }
    var isSelectingLocation by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    fun updateCount(newCount: Int) {
        if (newCount >= 1) {
            count = newCount
            onQuantityChange?.invoke(newCount)
        }
    }

    val selectedSpot = spots.firstOrNull { it.id == selectedSpotId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalViraExtraColors.current.surfaceElevated,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ViraSpacing.space24)
                .padding(top = ViraSpacing.space24, bottom = ViraSpacing.space32)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Nova recolha", style = ViraTypography.MetricMedium)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space24))

            // DOMINANT NUMBER STEPPER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LocalViraExtraColors.current.surfaceInteractive)
                        .clickable {
                            if (count > 1) {
                                updateCount(count - 1)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(ViraSpacing.space32))

                Text(
                    text = "$count",
                    style = ViraTypography.HeroNumber,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(ViraSpacing.space32))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LocalViraExtraColors.current.surfaceInteractive)
                        .clickable {
                            updateCount(count + 1)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Quick increments
            Spacer(modifier = Modifier.height(ViraSpacing.space16))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space8, Alignment.CenterHorizontally)
            ) {
                listOf(1, 5, 10, 25).forEach { inc ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ViraRadius.small))
                            .background(LocalViraExtraColors.current.surfaceInteractive)
                            .clickable {
                                updateCount(count + inc)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            .padding(horizontal = ViraSpacing.space16, vertical = ViraSpacing.space8)
                    ) {
                        Text(text = "+$inc", style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Potential financial value
            Spacer(modifier = Modifier.height(ViraSpacing.space12))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = FormatUtils.formatCurrency(count * DEPOSIT_VALUE_CENTS),
                    style = ViraTypography.MetricMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "valor potencial",
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space24))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(ViraSpacing.space16))

            // LOCATION EXPERIENCE
            Text(
                text = "LOCAL",
                style = ViraTypography.SectionTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ViraSpacing.space8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val locationTitle = when {
                        selectedSpot != null -> selectedSpot.name
                        pickedCoordinate != null -> "Localização guardada"
                        else -> "Sem localização"
                    }
                    Text(
                        text = locationTitle,
                        style = ViraTypography.Body,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedSpot?.address != null) {
                        Text(text = selectedSpot.address, style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (pickedCoordinate != null) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.4f, %.4f", pickedCoordinate.first, pickedCoordinate.second),
                            style = ViraTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { isSelectingLocation = !isSelectingLocation }) {
                    Text(
                        text = if (selectedSpot != null || pickedCoordinate != null) "Alterar →" else "Adicionar local →",
                        style = ViraTypography.ButtonLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // LOCATION SELECTION EXPANDABLE LIST
            AnimatedVisibility(visible = isSelectingLocation) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ViraSpacing.space8)
                        .clip(RoundedCornerShape(ViraRadius.medium))
                        .background(LocalViraExtraColors.current.surfaceInteractive)
                        .padding(ViraSpacing.space12)
                ) {
                    if (onUseCurrentLocation != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSelectingLocation = false
                                    selectedSpotId = null
                                    onQuantityChange?.invoke(count)
                                    onUseCurrentLocation()
                                }
                                .padding(vertical = ViraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(ViraSpacing.space8))
                            Text("Usar localização atual", style = ViraTypography.Body, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (onPickOnMap != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSelectingLocation = false
                                    onQuantityChange?.invoke(count)
                                    onPickOnMap()
                                }
                                .padding(vertical = ViraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(ViraSpacing.space8))
                            Text("Escolher no mapa →", style = ViraTypography.Body, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSpotId = null
                                isSelectingLocation = false
                            }
                            .padding(vertical = ViraSpacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(ViraSpacing.space8))
                        Text("Continuar sem localização", style = ViraTypography.BodySecondary)
                    }

                    spots.forEach { spot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSpotId = spot.id
                                    isSelectingLocation = false
                                }
                                .padding(vertical = ViraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(ViraSpacing.space8))
                            Text(spot.name, style = ViraTypography.Body)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ViraSpacing.space24))

            // PRIMARY ACTION WITH DYNAMIC PORTUGUESE GRAMMAR
            val ctaText = if (count == 1) "Guardar 1 embalagem" else "Guardar $count embalagens"
            ViraPrimaryButton(
                text = ctaText,
                onClick = {
                    onSave(count, selectedSpotId)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
