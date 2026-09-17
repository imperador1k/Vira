package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraIconSize
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

@Composable
fun ViraTimelineEntry(
    isCollection: Boolean,
    category: String,
    locationName: String,
    metricText: String,
    time: String,
    valueText: String,
    isLast: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = ViraSpacing.space8)
    ) {
        // Timeline spine (Dot + vertical connecting line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCollection) MaterialTheme.colorScheme.primary
                        else LocalViraExtraColors.current.warning
                    )
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(LocalViraExtraColors.current.border)
                )
            }
        }

        Spacer(modifier = Modifier.width(ViraSpacing.space16))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metricText,
                    style = ViraTypography.MetricMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = valueText,
                    style = ViraTypography.ButtonLabel,
                    color = if (isCollection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(ViraSpacing.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = locationName,
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = time,
                    style = ViraTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(ViraSpacing.space8))
        }
    }
}

@Composable
fun ViraSpotRow(
    name: String,
    totalContainers: Int,
    averagePerVisit: Double,
    lastVisitText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = ViraSpacing.space12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = ViraTypography.ButtonLabel, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(ViraSpacing.space4))
            Text(
                text = "$totalContainers embalagens · ${String.format("%.1f", averagePerVisit)} / visita",
                style = ViraTypography.BodySecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ViraSpacing.space4))
            Text(text = "Última visita · $lastVisitText", style = ViraTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ViraIconSize.small)
        )
    }
}
