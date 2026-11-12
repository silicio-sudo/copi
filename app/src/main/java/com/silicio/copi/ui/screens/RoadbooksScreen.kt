package com.silicio.copi.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.Roadbook
import com.silicio.copi.data.phraseEs
import com.silicio.copi.data.shortEs
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandFooter
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.NoteTypeIcon
import com.silicio.copi.ui.components.RouteCanvas
import com.silicio.copi.ui.components.SectionTitle
import com.silicio.copi.ui.components.StatCell
import com.silicio.copi.ui.components.StatusPill
import com.silicio.copi.ui.components.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoadbooksScreen(
    roadbooks: List<Roadbook>,
    settings: AppSettings,
    state: RallyState,
    importing: Boolean,
    onNavigateHome: () -> Unit,
    initialRoadbookId: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable(initialRoadbookId) { mutableStateOf(initialRoadbookId) }
    val selected = roadbooks.firstOrNull { it.id == selectedId }
    if (selected != null) {
        RoadbookDetail(
            roadbook = selected,
            settings = settings,
            state = state,
            onBack = { selectedId = null },
            onNavigateHome = onNavigateHome,
            modifier = modifier
        )
    } else {
        RoadbookList(
            roadbooks = roadbooks,
            settings = settings,
            state = state,
            importing = importing,
            onOpen = { selectedId = it.id },
            modifier = modifier
        )
    }
}

@Composable
private fun RoadbookList(
    roadbooks: List<Roadbook>,
    settings: AppSettings,
    state: RallyState,
    importing: Boolean,
    onOpen: (Roadbook) -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    var menuOpen by remember { mutableStateOf(false) }

    val gpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { Copi.importGpx(it) }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { Copi.importCsv(it) }
    }

    Box(modifier.fillMaxSize()) {
        LiveryBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            "TRAMOS",
                            style = MaterialTheme.typography.headlineLarge,
                            color = brand.text
                        )
                        Text(
                            "Tu biblioteca de roadbooks y pacenotes",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Importar tramo", tint = brand.primary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Importar GPX") },
                                onClick = {
                                    menuOpen = false
                                    gpxLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Importar roadbook CSV") },
                                onClick = {
                                    menuOpen = false
                                    csvLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        }
                    }
                }
            }

            if (importing) {
                item {
                    Column {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = brand.primary,
                            trackColor = brand.line
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Importando y generando pacenotes…",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted
                        )
                    }
                }
            }

            if (roadbooks.isEmpty()) {
                item {
                    CopiCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aún no hay tramos", style = MaterialTheme.typography.titleLarge, color = brand.text)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Importa un GPX, un CSV o planifica una ruta online desde la pestaña Ruta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.muted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { gpxLauncher.launch(arrayOf("*/*")) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.primary,
                                    contentColor = brand.onPrimary
                                )
                            ) { Text("Importar GPX") }
                        }
                    }
                }
            }

            items(roadbooks, key = { it.id }) { roadbook ->
                val isActive = state.roadbook?.id == roadbook.id
                CopiCard(
                    modifier = Modifier.clickable { onOpen(roadbook) },
                    highlight = isActive
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    roadbook.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = brand.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isActive) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusPill("ACTIVO", brand.ok)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${formatDistanceShort(roadbook.distanceM, settings.units)} · " +
                                    "${cornerCount(roadbook)} notas · ${sourceLabel(roadbook.source)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.muted
                            )
                            Text(
                                SimpleDateFormat("d MMM yyyy", Locale("es", "ES"))
                                    .format(Date(roadbook.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = brand.muted.copy(alpha = 0.65f)
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            null,
                            tint = brand.muted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            item { BrandFooter() }
        }
    }
}

@Composable
private fun RoadbookDetail(
    roadbook: Roadbook,
    settings: AppSettings,
    state: RallyState,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val isActive = state.roadbook?.id == roadbook.id
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LiveryBackdrop()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver", tint = brand.text)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            roadbook.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = brand.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${sourceLabel(roadbook.source)} · ${cornerCount(roadbook)} notas",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted
                        )
                    }
                    if (isActive) StatusPill("ACTIVO", brand.ok)
                }
            }

            item {
                RouteCanvas(
                    points = roadbook.route,
                    notes = roadbook.sortedNotes,
                    current = if (isActive) {
                        state.latitude?.let { lat -> state.longitude?.let { lon -> com.silicio.copi.data.GeoPoint(lat, lon) } }
                    } else null,
                    highlightIndex = if (isActive) state.nextIndex else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            item {
                CopiCard {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatCell("DISTANCIA", formatDistanceShort(roadbook.distanceM, settings.units))
                        StatCell("NOTAS", cornerCount(roadbook).toString())
                        StatCell("PUNTOS GPS", roadbook.route.size.toString())
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            Copi.activate(roadbook)
                            onNavigateHome()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.primary,
                            contentColor = brand.onPrimary
                        )
                    ) {
                        Text(if (isActive) "IR AL TRAMO" else "USAR TRAMO")
                    }
                    OutlinedButton(
                        onClick = {
                            Copi.activate(roadbook)
                            Copi.recalibrate()
                            Toast.makeText(context, "Alineando con el GPS…", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = "Alinear con el GPS",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = { Copi.shareCsv(roadbook) }) {
                        Icon(Icons.Filled.Share, "Compartir CSV", tint = brand.muted)
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, "Borrar", tint = brand.danger)
                    }
                }
            }

            item {
                SectionTitle("Pacenotes", "${roadbook.sortedNotes.size} registros")
            }

            items(roadbook.sortedNotes.withIndex().toList(), key = { it.index }) { indexed ->
                val note = indexed.value
                val isNext = isActive && indexed.index == state.nextIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isActive) {
                            Copi.selectNote(indexed.index)
                            Toast.makeText(
                                context,
                                "Siguiente: ${note.shortEs()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (indexed.index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNext) brand.accent else brand.muted,
                        modifier = Modifier.width(26.dp)
                    )
                    NoteTypeIcon(note, size = 22)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            note.shortEs(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isNext) brand.accent else brand.text,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitle = listOf(note.instruction, note.modifier)
                            .filter { value ->
                                value.isNotBlank() &&
                                    !value.equals(note.shortEs(), ignoreCase = true) &&
                                    !value.equals(note.phraseEs(), ignoreCase = true)
                            }
                            .joinToString(" · ")
                        if (subtitle.isNotBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        formatDistanceShort(note.distanceM, settings.units),
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted
                    )
                }
            }

            item { BrandFooter() }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Borrar tramo") },
            text = { Text("Se eliminará «${roadbook.name}» del dispositivo. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Copi.deleteRoadbook(roadbook.id)
                    onBack()
                }) { Text("Borrar", color = brand.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun cornerCount(roadbook: Roadbook): Int =
    roadbook.notes.count { it.type != NoteType.START && it.type != NoteType.FINISH }

private fun sourceLabel(source: String): String = when (source.lowercase(Locale.ROOT)) {
    "demo" -> "DEMO"
    "gpx" -> "GPX"
    "csv" -> "CSV"
    "osrm" -> "ONLINE"
    else -> "MANUAL"
}
