package com.silicio.copi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatSpeed
import com.silicio.copi.core.headingCardinal
import com.silicio.copi.core.speedUnit
import com.silicio.copi.core.speedValue
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.UnitSystem
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SpeedGauge(
    state: RallyState,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val maxDisplay = if (settings.units == UnitSystem.METRIC) 220f else 140f
    val value = speedValue(state.speedKmh, settings.units)
    val fraction = (value / maxDisplay).coerceIn(0f, 1f)

    Box(modifier.size(252.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2 + 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = brand.line.copy(alpha = 0.9f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = brand.primary,
                startAngle = 135f,
                sweepAngle = 270f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = brand.danger.copy(alpha = 0.55f),
                startAngle = 135f + 270f * 0.82f,
                sweepAngle = 270f * 0.18f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Butt)
            )

            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2 - inset + 2.dp.toPx()
            for (i in 0..10) {
                val angle = Math.toRadians((135f + 270f * i / 10f).toDouble())
                val outer = Offset(
                    center.x + cos(angle).toFloat() * radius,
                    center.y + sin(angle).toFloat() * radius
                )
                val innerRadius = radius - if (i % 5 == 0) 11.dp.toPx() else 6.dp.toPx()
                val inner = Offset(
                    center.x + cos(angle).toFloat() * innerRadius,
                    center.y + sin(angle).toFloat() * innerRadius
                )
                drawLine(
                    color = brand.muted.copy(alpha = if (i % 5 == 0) 0.55f else 0.25f),
                    start = inner,
                    end = outer,
                    strokeWidth = if (i % 5 == 0) 2f else 1f
                )
            }

            val needleAngle = Math.toRadians((135f + 270f * fraction).toDouble())
            val needleStart = Offset(
                center.x + cos(needleAngle).toFloat() * (radius - 28.dp.toPx()),
                center.y + sin(needleAngle).toFloat() * (radius - 28.dp.toPx())
            )
            val needleEnd = Offset(
                center.x + cos(needleAngle).toFloat() * (radius - 8.dp.toPx()),
                center.y + sin(needleAngle).toFloat() * (radius - 8.dp.toPx())
            )
            drawLine(
                color = brand.accent,
                start = needleStart,
                end = needleEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatSpeed(state.speedKmh, settings.units),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 74.sp),
                color = brand.text
            )
            Text(
                text = speedUnit(settings.units).uppercase(Locale("es", "ES")),
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted,
                letterSpacing = 2.4.sp
            )
            Text(
                text = "${headingCardinal(state.headingDeg.toDouble())} ${state.headingDeg.roundToInt()}°",
                style = MaterialTheme.typography.labelLarge,
                color = brand.accent,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
