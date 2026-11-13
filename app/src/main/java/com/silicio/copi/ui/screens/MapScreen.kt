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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.core.formatSpeed
import com.silicio.copi.core.isOnline
import com.silicio.copi.core.speedUnit
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.GeoPoint
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandEmblem
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.NextNoteCard
import com.silicio.copi.ui.components.RallyMap
import com.silicio.copi.ui.components.StatusPill

@Composable
fun MapScreen(
    state: RallyState,
    settings: AppSettings,
    onOpenRoadbooks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val roadbook = state.roadbook
    val current = state.latitude?.let { lat ->
        state.longitude?.let { lon -> GeoPoint(lat, lon) }
    }

    if (roadbook == null || roadbook.route.size < 2) {
        Box(modifier.fillMaxSize()) {
            LiveryBackdrop()
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandEmblem(size = 58)
                Spacer(Modifier.height(14.dp))
                Text(
                    "MAPA DEL TRAMO",
                    style = MaterialTheme.typography.headlineLarge,
                    color = brand.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (roadbook == null) {
                        "Elige un tramo con trazado GPS para verlo en el mapa."
                    } else {
                        "Este roadbook no tiene trazado GPS. Importa un GPX o genera una ruta online."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenRoadbooks,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand.primary,
                        contentColor = brand.onPrimary
                    )
                ) { Text("ELEGIR TRAMO") }
            }
        }
        return
    }

    Box(modifier.fillMaxSize()) {
        RallyMap(
            points = roadbook.route,
            current = current,
            followCurrent = state.running,
            modifier = Modifier.fillMaxSize()
        )

        // Degradado superior para que el HUD se lea sobre el mapa.
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(brand.bg.copy(alpha = 0.94f), Color.Transparent)
                    )
                )
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandEmblem(size = 34)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "MAPA",
                        style = MaterialTheme.typography.labelLarge,
                        color = brand.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        roadbook.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.text,
                        maxLines = 1
                    )
                }
                StatusPill(
                    text = if (state.demo) "DEMO" else if (state.gpsFix) "GPS OK" else "SIN GPS",
                    color = if (state.demo) brand.accent else if (state.gpsFix) brand.ok else brand.danger
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatSpeed(state.speedKmh, settings.units),
                    style = MaterialTheme.typography.displayMedium,
                    color = brand.text
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    speedUnit(settings.units),
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.muted
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (isOnline(context)) "© OpenStreetMap"
                    else "Sin conexión · teselas en caché",
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.muted
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, brand.bg.copy(alpha = 0.97f), brand.bg)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            NextNoteCard(state, settings)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        when {
                            state.finished -> {
                                Copi.resetNavigation()
                                Copi.startNavigation()
                            }
                            state.running && !state.paused -> Copi.pauseNavigation()
                            state.paused -> Copi.resumeNavigation()
                            else -> Copi.startNavigation()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brand.primary,
                        contentColor = brand.onPrimary
                    )
                ) {
                    Icon(
                        if (state.running && !state.paused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when {
                            state.paused -> "REANUDAR"
                            state.running -> "PAUSA"
                            state.finished -> "REINICIAR"
                            else -> "INICIAR TRAMO"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { Copi.recalibrate() },
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Alinear posición",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
