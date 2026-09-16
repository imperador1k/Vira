package com.example.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalViraExtraColors
import com.example.ui.theme.ViraRadius
import com.example.ui.theme.ViraSpacing
import com.example.ui.theme.ViraTypography

@Composable
fun ViraSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ViraRadius.large),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(LocalViraExtraColors.current.surfaceElevated)
            .padding(ViraSpacing.space24),
        content = content
    )
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
                style = ViraTypography.SectionTitle,
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
    Canvas(modifier = modifier.size(96.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 1.dp.toPx()

        // Concentric geospatial rings
        drawCircle(
            color = subtleColor,
            radius = size.minDimension * 0.44f,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = subtleColor,
            radius = size.minDimension * 0.28f,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = subtleColor,
            radius = size.minDimension * 0.15f,
            style = Stroke(width = strokeWidth)
        )

        // Precision crosshair ticks
        val tickLen = 6.dp.toPx()
        val outerR = size.minDimension * 0.44f
        // North
        drawLine(subtleColor, Offset(center.x, center.y - outerR - tickLen), Offset(center.x, center.y - outerR + tickLen), strokeWidth)
        // South
        drawLine(subtleColor, Offset(center.x, center.y + outerR - tickLen), Offset(center.x, center.y + outerR + tickLen), strokeWidth)
        // West
        drawLine(subtleColor, Offset(center.x - outerR - tickLen, center.y), Offset(center.x - outerR + tickLen, center.y), strokeWidth)
        // East
        drawLine(subtleColor, Offset(center.x + outerR - tickLen, center.y), Offset(center.x + outerR + tickLen, center.y), strokeWidth)

        // Subtle accent arc
        drawArc(
            color = accentColor.copy(alpha = 0.6f),
            startAngle = -45f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(center.x - outerR, center.y - outerR),
            size = Size(outerR * 2, outerR * 2),
            style = Stroke(width = 2.dp.toPx())
        )

        // Central beacon pulse & core dot
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

@Composable
fun ViraEmptyState(
    title: String,
    subtitle: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ViraSpacing.space24),
        horizontalAlignment = Alignment.Start
    ) {
        ViraMapCompassMotif()
        Spacer(modifier = Modifier.height(ViraSpacing.space24))
        Text(
            text = title,
            style = ViraTypography.MetricLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(ViraSpacing.space12))
        Text(
            text = subtitle,
            style = ViraTypography.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(ViraSpacing.space24))
        ViraPrimaryButton(
            text = ctaText,
            onClick = onCtaClick,
            modifier = Modifier.fillMaxWidth()
        )
        if (footnote != null) {
            Spacer(modifier = Modifier.height(ViraSpacing.space16))
            Text(
                text = footnote,
                style = ViraTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
