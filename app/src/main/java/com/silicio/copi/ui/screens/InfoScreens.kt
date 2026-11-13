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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.BuildConfig
import com.silicio.copi.nav.Copi
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandFooter
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.BrandEmblem
import com.silicio.copi.ui.components.SectionTitle
import com.silicio.copi.ui.components.StatusPill
import com.silicio.copi.ui.components.WordmarkMark

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val uriHandler = LocalUriHandler.current
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
                    Text(
                        "ACERCA DE",
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted,
                        letterSpacing = 2.4.sp
                    )
                }
            }

            item {
                CopiCard(highlight = true) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrandEmblem(size = 64)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            brand.name.uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = brand.text,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            brand.edition,
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.primary,
                            letterSpacing = 2.6.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            brand.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        if (brand.showPersonalization && brand.driver.isNotBlank()) {
                            Spacer(Modifier.height(18.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(brand.surfaceAlt, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        "PILOTO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = brand.muted,
                                        letterSpacing = 2.2.sp
                                    )
                                    Text(
                                        brand.driver,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Black,
                                        color = brand.text,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable(enabled = brand.url.isNotBlank()) {
                                                if (brand.url.isNotBlank()) uriHandler.openUri(brand.url)
                                            }
                                    ) {
                                        Icon(
                                            Icons.Filled.Code,
                                            null,
                                            tint = brand.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            brand.handle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = brand.accent
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "v${BuildConfig.VERSION_NAME.removeSuffix("-debug")} · " +
                                BuildConfig.APPLICATION_ID,
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Qué hace Copi")
                        FeatureLine("• Genera pacenotes 1–6 desde un GPX o una ruta online.")
                        FeatureLine("• Navega el tramo con GPS, distancia, media y cronómetro.")
                        FeatureLine("• Canta las notas con voz natural local o en la nube.")
                        FeatureLine("• Roadbooks CSV, biblioteca local y modo demo.")
                        FeatureLine("• Android Auto con HUD, notas y controles en el coche.")
                        FeatureLine("• Todo el tramo funciona offline; internet solo para extras.")
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Privacidad")
                        Text(
                            "La ubicación se procesa en el dispositivo y no se envía a ningún servidor de Copi " +
                                "(no existe backend). Las rutas online consultan OSM/OSRM y la búsqueda, Nominatim. " +
                                "Las claves de voz se guardan en el almacenamiento privado de la app, sin cifrado " +
                                "adicional: protégelas como cualquier credencial del dispositivo. " +
                                "No hay analítica, publicidad ni cuentas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Tecnología y licencias")
                        Text(
                            "Kotlin + Jetpack Compose + Android for Cars App Library. " +
                                "Mapas y geocodificación © OpenStreetMap contributors. " +
                                "Rutas OSRM. Voces opcionales Google Cloud TTS / ElevenLabs. " +
                                "El código de Copi se publica bajo licencia MIT.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    if (brand.showPersonalization) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    if (brand.url.isNotBlank()) uriHandler.openUri(brand.url)
                                }
                            ) {
                                Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("GitHub · silicio-sudo")
                            }
                        }
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Aviso legal")
                        Text(
                            brand.legal,
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Conduce con prudencia. Las pacenotes son una ayuda: respeta las normas de tráfico " +
                                "y no manipules el móvil mientras conduces. Copi no sustituye al copiloto humano.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        Spacer(Modifier.height(8.dp))
                        StatusPill("MIT · OFFLINE-FIRST", brand.accent)
                    }
                }
            }

            item { BrandFooter() }
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    val brand = LocalBrand.current
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = brand.muted,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val uriHandler = LocalUriHandler.current
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
                    Column {
                        Text("MANUAL RÁPIDO", style = MaterialTheme.typography.labelSmall, color = brand.muted, letterSpacing = 2.4.sp)
                        Text("Primeros pasos", style = MaterialTheme.typography.titleLarge, color = brand.text)
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("1 · Prepara el tramo")
                        Text(
                            "• Importar GPX: pestaña Tramo → + → Importar GPX. Copi detecta curvas y genera " +
                                "notas 1 (horquilla) a 6 (rápida).\n" +
                                "• CSV: columnas distancia_m,tipo,severidad,modificador,nota. " +
                                "Acepta distancias absolutas o relativas con +.\n" +
                                "• Online: pestaña Ruta → busca destino → Generar tramo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("2 · Inicia y calibra")
                        Text(
                            "Colócate en la salida del tramo y pulsa INICIAR TRAMO. Copi acumula distancia GPS. " +
                                "Si empiezas a mitad de tramo, pulsa ALINEAR: encaja tu posición sobre el trazado. " +
                                "ANTELACIÓN controla cuántos segundos antes se canta la nota. " +
                                "El botón de nota siguiente salta una nota; el anterior retrocede.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("3 · Voz natural")
                        Text(
                            "La voz local elige la mejor voz instalada en español (prefiere neuronales de alta " +
                                "calidad). Para una voz aún más humana, añade una clave de Google Cloud TTS o " +
                                "ElevenLabs en Ajustes → Voces en la nube. Precarga el tramo para conservar " +
                                "frases y poder repetirlas sin conexión. Si la nube falla, Copi no se queda " +
                                "mudo: cambia a la voz local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("4 · Android Auto")
                        Text(
                            "1. Instala Copi en el móvil y abre la app una vez.\n" +
                                "2. Activa el modo desarrollador de Android Auto (toca 10 veces la versión) y " +
                                "marca «Fuentes desconocidas».\n" +
                                "3. Conecta por USB; Copi aparecerá en el lanzador de apps del coche.\n" +
                                "4. En el coche verás el HUD de velocidad, la siguiente nota y controles.\n\n" +
                                "Para distribución en Google Play, la categoría Navegación requiere aprobación " +
                                "de Google. Un APK sideloaded funciona en modo desarrollador/DHU, no en todos " +
                                "los coches de producción.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("5 · Solución de problemas")
                        Text(
                            "• Sin GPS: concede Ubicación precisa y activa el GPS; prueba Modo demo.\n" +
                                "• Sin voz: revisa el motor TTS, descarga español, sube el volumen multimedia.\n" +
                                "• La distancia se desvía: pulsa ALINEAR cerca del trazado.\n" +
                                "• Android Auto no muestra Copi: activa fuentes desconocidas y el modo desarrollador.\n" +
                                "• Los servicios online fallan: usa tu propio OSRM/Nominatim en Ajustes → Avanzado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Manual completo")
                        Text(
                            "El manual de uso y configuración completo está en MANUAL.md, dentro del proyecto. " +
                                "Incluye formato de archivos, ajustes, Android Auto/DHU, voz y preguntas frecuentes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        if (brand.showPersonalization) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (brand.url.isNotBlank()) {
                                        uriHandler.openUri(brand.url.trimEnd('/') + "/copi/blob/main/MANUAL.md")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Abrir manual en GitHub")
                            }
                        }
                    }
                }
            }

            item { BrandFooter() }
        }
    }
}
