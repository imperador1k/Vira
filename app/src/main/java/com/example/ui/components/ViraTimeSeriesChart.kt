package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ViraTimeSeriesChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    onPointSelected: ((Int, Float) -> Unit)? = null
) {
    if (data.size < 2) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minVal = (data.minOrNull() ?: 0f).coerceAtMost(0f)
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .pointerInput(data) {
                detectTapGestures(
                    onPress = { offset ->
                        val stepX = size.width / (data.size - 1)
                        val idx = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = idx
                        onPointSelected?.invoke(idx, data[idx])
                    }
                )
            }
            .pointerInput(data) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val stepX = size.width / (data.size - 1)
                        val idx = ((change.position.x + stepX / 2) / stepX).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = idx
                        onPointSelected?.invoke(idx, data[idx])
                    },
                    onDragEnd = { selectedIndex = null }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)

        // Subtle horizontal guide axes
        drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, h * 0.3f), Offset(w, h * 0.3f), 1.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, h * 0.7f), Offset(w, h * 0.7f), 1.dp.toPx())

        val points = data.mapIndexed { i, v ->
            val x = i * stepX
            val normY = 1f - ((v - minVal) / range)
            val y = (normY * (h - 24.dp.toPx())) + 12.dp.toPx()
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        selectedIndex?.let { idx ->
            val p = points[idx]
            drawLine(lineColor.copy(alpha = 0.4f), Offset(p.x, 0f), Offset(p.x, h), 1.dp.toPx())
            drawCircle(lineColor.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = p)
            drawCircle(lineColor, radius = 4.5.dp.toPx(), center = p)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = p)
        }
    }
}
