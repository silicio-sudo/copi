package com.silicio.copi.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.AppSettings
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandFooter
import com.silicio.copi.ui.components.BrandHeader
import com.silicio.copi.ui.components.ControlBar
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.NextNoteCard
import com.silicio.copi.ui.components.SpeedGauge
import com.silicio.copi.ui.components.StageProgress
import com.silicio.copi.ui.components.StageStats
import com.silicio.copi.ui.components.StatusPill
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(
    state: RallyState,
    settings: AppSettings,
    hasLocation: Boolean,
    onRequestPermission: () -> Unit,
    onOpenRoadbooks: () -> Unit,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val online = isOnline(context)
    var confirmReset by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LiveryBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            BrandHeader(state)
            Spacer(Modifier.height(12.dp))

            if (!hasLocation && !settings.demoMode) {
                CopiCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(brand.accent.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MyLocation, null, tint = brand.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Activa el GPS",
                                style = MaterialTheme.typography.titleLarge,
                                color = brand.text
                            )
                            Text(
                                "Sin ubicación no se pueden cantar las notas. También puedes probar en modo demo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.muted
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brand.primary,
                                contentColor = brand.onPrimary
                            )
                        ) {
                            Text("Conceder permiso")
                        }
                        OutlinedButton(onClick = { Copi.setDemoMode(true) }) {
                            Text("Modo demo")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.roadbook == null) {
                CopiCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .background(brand.primary.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Map, null, tint = brand.primary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "PREPARA EL PRIMER TRAMO",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.primary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Elige un roadbook, importa un GPX o deja que Copi genere los pacenotes de una ruta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenRoadbooks,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.primary,
                                    contentColor = brand.onPrimary
                                )
                            ) {
                                Text("Elegir tramo")
                            }
                            OutlinedButton(onClick = onOpenPlanner) {
                                Text("Planificar")
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SpeedGauge(state, settings)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = if (state.running) {
                            if (state.paused) "EN PAUSA" else "EN TRAMO"
                        } else {
                            "LISTO"
                        },
                        color = when {
                            state.running && !state.paused -> brand.ok
                            state.paused -> brand.accent
                            else -> brand.muted
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    if (state.demo) {
                        StatusPill("DEMO", brand.accent)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.roadbook.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Spacer(Modifier.height(10.dp))

                NextNoteCard(state, settings)
                Spacer(Modifier.height(14.dp))
                StageProgress(state, settings)
                Spacer(Modifier.height(14.dp))
                CopiCard {
                    StageStats(state, settings)
                }
                Spacer(Modifier.height(14.dp))
                ControlBar(
                    state = state,
                    settings = settings,
                    onPrimary = {
                        when {
                            state.finished -> {
                                Copi.resetNavigation()
                                Copi.startNavigation()
                            }
                            state.running && !state.paused -> Copi.pauseNavigation()
                            state.paused -> Copi.resumeNavigation()
                            !hasLocation && !settings.demoMode -> onRequestPermission()
                            else -> Copi.startNavigation()
                        }
                    },
                    onReset = { confirmReset = true },
                    onPrevious = { Copi.previousNote() },
                    onNext = { Copi.skipNext() },
                    onMute = { Copi.toggleMute() },
                    onRecalibrate = {
                        if (state.running) Copi.recalibrate()
                    }
                )

                if (!online) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brand.accent.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.GpsFixed, null, tint = brand.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sin conexión: GPS, roadbook y voz local siguen funcionando.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            BrandFooter()
            Spacer(Modifier.height(18.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reiniciar tramo") },
            text = { Text("Se borrarán el tiempo, la distancia y el avance de notas actuales.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    Copi.resetNavigation()
                }) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar") }
            }
        )
    }
}
