package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

/**
 * Premium Segmented Control for Period and Category Filtering
 */
@Composable
fun ViraPeriodSelector(
    periods: List<String>,
    selectedIndex: Int,
    onSelectPeriod: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ViraRadius.medium)),
        color = LocalViraExtraColors.current.surfaceElevated,
        shape = RoundedCornerShape(ViraRadius.medium),
        border = BorderStroke(1.dp, LocalViraExtraColors.current.cardBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            periods.forEachIndexed { index, period ->
                val isSelected = index == selectedIndex
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) LocalViraExtraColors.current.cardBackground else Color.Transparent,
                    animationSpec = tween(180),
                    label = "PeriodBackgroundAnimation"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "PeriodTextAnimation"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(ViraRadius.small))
                        .background(backgroundColor)
                        .then(
                            if (isSelected) Modifier.border(
                                1.dp,
                                LocalViraExtraColors.current.cardBorder,
                                RoundedCornerShape(ViraRadius.small)
                            ) else Modifier
                        )
                        .clickable { onSelectPeriod(index) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        style = ViraTypography.Caption.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}
