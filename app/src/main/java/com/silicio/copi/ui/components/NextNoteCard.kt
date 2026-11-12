package com.silicio.copi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.phraseEs
import com.silicio.copi.data.shortEs
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand

@Composable
fun NextNoteCard(
    state: RallyState,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val note = state.nextNote
    when {
        state.roadbook == null -> CopiCard(modifier) {
            EmptyHint(
                title = "Sin tramo activo",
                text = "Importa un GPX, crea un roadbook CSV o planifica una ruta para empezar.",
                icon = Icons.Filled.Map
            )
        }
        state.finished -> CopiCard(modifier, highlight = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TRAMO COMPLETADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.ok,
                    letterSpacing = 2.4.sp
                )
                Text(
                    "META",
                    style = MaterialTheme.typography.displayMedium,
                    color = brand.text
                )
                Text(
                    "${com.silicio.copi.core.formatDuration(state.elapsedMs)} · " +
                        "${formatDistanceShort(state.stageDistanceM, settings.units)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand.muted
                )
            }
        }
        note == null -> CopiCard(modifier) {
            EmptyHint(
                title = "Sin notas pendientes",
                text = "Has pasado la última nota. Pulsa reiniciar para repetir el tramo.",
                icon = Icons.Filled.Flag
            )
        }
        else -> CopiCard(modifier, highlight = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(brand.primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    NoteTypeIcon(note, size = 34)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = state.distanceToNextM?.let {
                                formatDistanceShort(it, settings.units)
                            } ?: "—",
                            style = MaterialTheme.typography.headlineLarge,
                            color = brand.primary
                        )
                        if (note.severity in 1..6) {
                            Spacer(Modifier.width(8.dp))
                            SeverityBadge(note.severity)
                        }
                    }
                    Text(
                        text = note.shortEs(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black,
                        color = brand.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.modifier.isNotBlank() || note.extra.isNotBlank()) {
                        Text(
                            text = listOf(note.modifier, note.extra)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.accent
                        )
                    } else if (note.instruction.isNotBlank() && note.instruction != note.phraseEs()) {
                        Text(
                            text = note.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${state.noteNumber}/${state.totalNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted
                    )
                    Spacer(Modifier.size(6.dp))
                    StatusPill(
                        text = if (state.paused) "PAUSA" else "SIGUIENTE",
                        color = if (state.paused) brand.accent else brand.muted
                    )
                }
            }
            val next = state.upcoming.drop(1).firstOrNull()
            if (next != null) {
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brand.surfaceAlt.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DESPUÉS",
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        next.shortEs(),
                        style = MaterialTheme.typography.labelLarge,
                        color = brand.text
                    )
                    Spacer(Modifier.weight(1f))
                    if (next.modifier.isNotBlank()) {
                        Text(
                            next.modifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: Int) {
    val brand = LocalBrand.current
    Box(
        Modifier
            .background(brand.accent.copy(alpha = 0.15f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = "G$severity",
            style = MaterialTheme.typography.labelSmall,
            color = brand.accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RoadbookStats(roadbookDistanceM: Double, notes: List<PaceNote>, settings: AppSettings) {
    val brand = LocalBrand.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatCell("DISTANCIA", formatDistanceShort(roadbookDistanceM, settings.units))
        StatCell("NOTAS", notes.count { it.type != com.silicio.copi.data.NoteType.START && it.type != com.silicio.copi.data.NoteType.FINISH }.toString())
    }
    Text(
        "Toca una nota para colocarla como siguiente",
        style = MaterialTheme.typography.bodySmall,
        color = brand.muted
    )
}
