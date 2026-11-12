package com.silicio.copi.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.Place
import com.silicio.copi.data.Roadbook
import com.silicio.copi.data.RoutePlanner
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandFooter
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.RouteCanvas
import com.silicio.copi.ui.components.SectionTitle
import com.silicio.copi.ui.components.StatCell
import com.silicio.copi.ui.components.StatusPill
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PlannerScreen(
    settings: AppSettings,
    state: RallyState,
    onGenerated: (Roadbook) -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val networkOnline = isOnline(context)
    val online = networkOnline && settings.onlineServices
    val scope = rememberCoroutineScope()
    val pending by Copi.pendingDestination.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Roadbook?>(null) }

    LaunchedEffect(pending) {
        val query = pending
        if (!query.isNullOrBlank()) {
            destination = query
            selectedPlace = null
            preview = null
            Copi.setPendingDestination(null)
        }
    }

    fun search() {
        if (destination.isBlank() || searching) return
        searching = true
        error = null
        scope.launch {
            runCatching {
                RoutePlanner { Copi.settings.current }.geocode(destination)
            }.onSuccess {
                places = it
                if (it.isEmpty()) error = "No se encontraron resultados para «$destination»"
            }.onFailure {
                error = "Búsqueda: ${it.message}"
            }
            searching = false
        }
    }

    fun plan() {
        if (destination.isBlank() || busy) return
        busy = true
        error = null
        preview = null
        val query = selectedPlace?.let {
            String.format(Locale.US, "%.6f, %.6f", it.lat, it.lon)
        } ?: destination
        scope.launch {
            runCatching {
                Copi.planRoute(query, origin.ifBlank { null })
            }.onSuccess {
                preview = it
                places = emptyList()
            }.onFailure {
                error = "No se pudo generar el tramo: ${it.message}"
            }
            busy = false
        }
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
                Column {
                    Text("RUTA", style = MaterialTheme.typography.headlineLarge, color = brand.text)
                    Text(
                        "Copi convierte la carretera en pacenotes 1–6",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.muted
                    )
                }
            }

            if (!online) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brand.accent.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CloudOff, null, tint = brand.accent)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (!networkOnline) {
                                "Sin conexión. La planificación online está desactivada; importa un GPX o CSV para seguir en offline."
                            } else {
                                "Servicios online desactivados en Ajustes. Actívalos para buscar y generar rutas."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.accent
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Planificador online")
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {
                                destination = it
                                selectedPlace = null
                                error = null
                            },
                            label = { Text("Destino (dirección, lugar o lat,lon)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = { Text("Origen (vacío = mi GPS actual)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = ::search,
                                enabled = online && destination.isNotBlank() && !searching,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (searching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = brand.primary
                                    )
                                } else {
                                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Buscar")
                                }
                            }
                            Button(
                                onClick = ::plan,
                                enabled = online && destination.isNotBlank() && !busy,
                                modifier = Modifier.weight(1.4f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.primary,
                                    contentColor = brand.onPrimary
                                )
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = brand.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (busy) "GENERANDO…" else "GENERAR TRAMO")
                            }
                        }
                        if (!online) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (!networkOnline) {
                                    "Conecta a internet o usa la importación offline."
                                } else {
                                    "Activa Servicios online en Ajustes para planificar."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.accent
                            )
                        }
                    }
                }
            }

            if (places.isNotEmpty()) {
                item { SectionTitle("Resultados", "${places.size} coincidencias") }
                items(places) { place ->
                    val selected = selectedPlace == place
                    CopiCard(
                        modifier = Modifier.clickable {
                            selectedPlace = place
                            destination = place.name
                        },
                        highlight = selected
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.Explore,
                                contentDescription = null,
                                tint = if (selected) brand.ok else brand.muted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    place.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = brand.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    place.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = brand.muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.danger
                    )
                }
            }

            if (preview != null) {
                val rb = preview!!
                item {
                    CopiCard(highlight = true) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "TRAMO GENERADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = brand.ok,
                                    letterSpacing = 2.sp
                                )
                                Spacer(Modifier.weight(1f))
                                StatusPill("ONLINE", brand.primary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                rb.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = brand.text
                            )
                            Spacer(Modifier.height(10.dp))
                            RouteCanvas(
                                points = rb.route,
                                notes = rb.sortedNotes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                StatCell(
                                    "DISTANCIA",
                                    formatDistanceShort(rb.distanceM, settings.units)
                                )
                                StatCell(
                                    "CURVAS",
                                    rb.notes.count {
                                        it.type != com.silicio.copi.data.NoteType.START &&
                                            it.type != com.silicio.copi.data.NoteType.FINISH
                                    }.toString()
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        Copi.activate(rb)
                                        onGenerated(rb)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brand.primary,
                                        contentColor = brand.onPrimary
                                    )
                                ) { Text("USAR ESTE TRAMO") }
                                TextButton(onClick = { preview = null }) {
                                    Text("Descartar")
                                }
                            }
                        }
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Offline")
                        Text(
                            "El tramo generado se guarda en el dispositivo y funciona sin conexión. " +
                                "La voz local y el GPS no necesitan internet. " +
                                "Para voces en la nube, precarga las frases desde Ajustes → Voz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Datos de mapas © OpenStreetMap contributors · Rutas OSRM",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item { BrandFooter() }
        }
    }
}
