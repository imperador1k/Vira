package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ViraIconSize
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

@Composable
fun ViraTopBar(
    title: String = "Vira",
    subtitle: String? = null,
    onProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.space16),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = ViraTypography.PageTitle,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(ViraSpacing.space4))
                Text(
                    text = subtitle,
                    style = ViraTypography.BodySecondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onProfileClick != null) {
            ViraIconButton(
                icon = Icons.Default.Person,
                contentDescription = "Perfil",
                onClick = onProfileClick
            )
        }
    }
}

@Composable
fun ViraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = ViraTypography.SectionTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = ViraTypography.Caption,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onActionClick)
                    .padding(vertical = ViraSpacing.space4)
            )
        }
    }
}

@Composable
fun ViraMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = ViraTypography.HeroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(ViraSpacing.space4))
        Text(
            text = value,
            style = ViraTypography.MetricLarge,
            color = valueColor
        )
        if (secondaryText != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space4))
            Text(
                text = secondaryText,
                style = ViraTypography.BodySecondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
