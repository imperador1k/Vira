package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

@Composable
fun ViraPeriodSelector(
    periods: List<String>,
    selectedIndex: Int,
    onSelectPeriod: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ViraRadius.medium))
            .background(LocalViraExtraColors.current.surfaceInteractive)
            .padding(ViraSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.space4)
    ) {
        periods.forEachIndexed { index, period ->
            val isSelected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) LocalViraExtraColors.current.surfaceElevated else Color.Transparent,
                animationSpec = tween(200),
                label = "PeriodBackgroundAnimation"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "PeriodTextAnimation"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(ViraRadius.small))
                    .background(backgroundColor)
                    .clickable { onSelectPeriod(index) }
                    .padding(vertical = ViraSpacing.space8),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    style = ViraTypography.Caption,
                    color = textColor
                )
            }
        }
    }
}
