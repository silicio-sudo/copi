package com.silicio.copi.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.formatDuration
import com.silicio.copi.core.formatSpeed
import com.silicio.copi.core.speedUnit
import com.silicio.copi.data.AppSettings
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import kotlin.math.roundToInt

@Composable
fun StageStats(state: RallyState, settings: AppSettings, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCell("TIEMPO", formatDuration(state.elapsedMs))
        StatCell("MEDIA", formatSpeed(state.avgSpeedKmh, settings.units), speedUnit(settings.units))
        StatCell("RECORRIDO", formatDistanceShort(state.stageDistanceM, settings.units))
        StatCell("MÁX", formatSpeed(state.maxSpeedKmh, settings.units), speedUnit(settings.units))
    }
}

@Composable
fun StageProgress(state: RallyState, settings: AppSettings, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val total = state.roadbook?.distanceM ?: 0.0
    val notes = state.roadbook?.sortedNotes.orEmpty()
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TRAMO",
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = brand.accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val centerY = size.height / 2
            drawLine(
                color = brand.line,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = brand.primary,
                start = Offset(0f, centerY),
                end = Offset(size.width * state.progress.coerceIn(0f, 1f), centerY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            if (total > 1.0) {
                notes.forEachIndexed { index, note ->
                    val x = (note.distanceM / total).toFloat().coerceIn(0f, 1f) * size.width
                    val isNext = index == state.nextIndex
                    drawCircle(
                        color = when {
                            isNext -> brand.accent
                            note.distanceM <= state.stageDistanceM -> brand.primary
                            else -> brand.muted.copy(alpha = 0.55f)
                        },
                        radius = if (isNext) 5.dp.toPx() else 2.4.dp.toPx(),
                        center = Offset(x, centerY)
                    )
                }
            }
        }
        Row {
            Text(
                formatDistanceShort(state.stageDistanceM, settings.units),
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatDistanceShort(total, settings.units),
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted
            )
        }
    }
}

@Composable
fun ControlBar(
    state: RallyState,
    settings: AppSettings,
    onPrimary: () -> Unit,
    onReset: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMute: () -> Unit,
    onRecalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val primaryLabel = when {
        state.paused -> "REANUDAR"
        state.running -> "PAUSA"
        state.finished -> "REINICIAR"
        else -> "INICIAR TRAMO"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(46.dp)
                .background(brand.surfaceAlt, CircleShape)
        ) {
            Icon(Icons.Filled.SkipPrevious, "Nota anterior", tint = brand.text)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = brand.primary,
                contentColor = brand.onPrimary
            )
        ) {
            Icon(
                imageVector = if (state.running && !state.paused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                primaryLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(46.dp)
                .background(brand.surfaceAlt, CircleShape)
        ) {
            Icon(Icons.Filled.SkipNext, "Nota siguiente", tint = brand.text)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SmallAction(
            icon = Icons.Filled.RestartAlt,
            label = "Reiniciar",
            onClick = onReset,
            modifier = Modifier.weight(1f)
        )
        SmallAction(
            icon = Icons.Filled.MyLocation,
            label = "Alinear",
            onClick = onRecalibrate,
            modifier = Modifier.weight(1f)
        )
        SmallAction(
            icon = if (settings.voiceEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            label = if (settings.voiceEnabled) "Voz ON" else "Voz OFF",
            onClick = onMute,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (state.gpsFix) "±${state.accuracyM.roundToInt()} m" else "",
            style = MaterialTheme.typography.labelSmall,
            color = brand.muted,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    Row(
        modifier = modifier
            .background(brand.surfaceAlt.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = brand.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = brand.muted, maxLines = 1)
    }
}
