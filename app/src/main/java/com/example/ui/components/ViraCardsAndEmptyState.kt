package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

/**
 * Vira Surface Card — Core container for the Vira Design System.
 * Light, modern, tactile, with subtle hairlines and generous breathing room.
 */
@Composable
fun ViraSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ViraRadius.large),
    border: BorderStroke? = BorderStroke(1.dp, LocalViraExtraColors.current.cardBorder),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = LocalViraExtraColors.current.cardBackground,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Compact Vira Card (for bento grids or secondary lists)
 */
@Composable
fun ViraCompactCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ViraRadius.medium),
    border: BorderStroke? = BorderStroke(1.dp, LocalViraExtraColors.current.cardBorder),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier,
        shape = shape,
        color = LocalViraExtraColors.current.cardBackground,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Vira Stat Card — Presents a clean metric with eyebrow tag, prominent number, and contextual subtitle.
 */
@Composable
fun ViraStatCard(
    eyebrow: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    ViraCompactCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = ViraTypography.Eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (badgeText != null) {
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(ViraRadius.small)
                ) {
                    Text(
                        text = badgeText,
                        style = ViraTypography.Caption.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(ViraSpacing.space8))
        Text(
            text = value,
            style = ViraTypography.DisplayMedium.copy(fontSize = 28.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space4))
            Text(
                text = subtitle,
                style = ViraTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun ViraInsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    ViraSurfaceCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(ViraSpacing.space8))
            Text(
                text = title.uppercase(),
                style = ViraTypography.Eyebrow,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(ViraSpacing.space8))
        Text(
            text = description,
            style = ViraTypography.Body,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space12))
            Text(
                text = actionText,
                style = ViraTypography.ButtonLabel,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Composable
fun ViraMapCompassMotif(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    subtleColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
) {
    Canvas(modifier = modifier.size(56.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        drawCircle(
            color = subtleColor,
            radius = size.minDimension * 0.44f,
            style = Stroke(width = strokeWidth)
        )

        val tickLen = 6.dp.toPx()
        val outerR = size.minDimension * 0.44f
        drawLine(subtleColor, Offset(center.x, center.y - outerR - tickLen), Offset(center.x, center.y - outerR + tickLen), strokeWidth)
        drawLine(subtleColor, Offset(center.x, center.y + outerR - tickLen), Offset(center.x, center.y + outerR + tickLen), strokeWidth)
        drawLine(subtleColor, Offset(center.x - outerR - tickLen, center.y), Offset(center.x - outerR + tickLen, center.y), strokeWidth)
        drawLine(subtleColor, Offset(center.x + outerR - tickLen, center.y), Offset(center.x + outerR + tickLen, center.y), strokeWidth)

        drawArc(
            color = accentColor.copy(alpha = 0.6f),
            startAngle = -45f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(center.x - outerR, center.y - outerR),
            size = Size(outerR * 2, outerR * 2),
            style = Stroke(width = 2.dp.toPx())
        )

        drawCircle(
            color = accentColor.copy(alpha = 0.2f),
            radius = 9.dp.toPx()
        )
        drawCircle(
            color = accentColor,
            radius = 3.5.dp.toPx()
        )
    }
}

/**
 * Branded Vira Empty State Component
 */
@Composable
fun ViraEmptyState(
    title: String,
    subtitle: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    footnote: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.space32),
        horizontalAlignment = Alignment.Start
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = LocalViraExtraColors.current.surfaceInteractive,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            ViraMapCompassMotif()
        }
        Spacer(modifier = Modifier.height(ViraSpacing.space24))
        Text(
            text = title,
            style = ViraTypography.ScreenTitle,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(ViraSpacing.space12))
        Text(
            text = subtitle,
            style = ViraTypography.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space24))
            ViraHeroButton(
                text = ctaText,
                onClick = onCtaClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (footnote != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space16))
            Text(
                text = footnote,
                style = ViraTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
