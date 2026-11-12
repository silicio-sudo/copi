package com.silicio.copi.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silicio.copi.BuildConfig
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.UnitSystem
import com.silicio.copi.data.VoiceMode
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.PrecacheProgress
import com.silicio.copi.nav.VoiceOption
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.components.BrandFooter
import com.silicio.copi.ui.components.CopiCard
import com.silicio.copi.ui.components.LiveryBackdrop
import com.silicio.copi.ui.components.SectionTitle
import com.silicio.copi.ui.components.formatBytes
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val voiceStatus by Copi.voice.status.collectAsStateWithLifecycle()
    val precache by Copi.voice.precache.collectAsStateWithLifecycle()
    var showVoices by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableStateOf(Copi.voice.cloudCacheBytes()) }
    val online = isOnline(context)

    LaunchedEffect(precache.running, precache.total) {
        if (!precache.running) {
            cacheBytes = Copi.voice.cloudCacheBytes()
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
                    Text("AJUSTES", style = MaterialTheme.typography.headlineLarge, color = brand.text)
                    Text(
                        "Navegación, voz y conexión",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.muted
                    )
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Navegación")
                        OptionChips(
                            label = "Unidades",
                            options = listOf(
                                "km/h" to (settings.units == UnitSystem.METRIC),
                                "mph" to (settings.units == UnitSystem.IMPERIAL)
                            ),
                            onSelect = { index ->
                                Copi.updateSettings {
                                    it.copy(
                                        units = if (index == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        SliderSetting(
                            title = "Antelación de la voz",
                            valueLabel = "${settings.lookaheadSeconds} s",
                            value = settings.lookaheadSeconds.toFloat(),
                            range = 3f..10f,
                            steps = 6,
                            onChange = { v ->
                                Copi.updateSettings { it.copy(lookaheadSeconds = v.roundToInt()) }
                            }
                        )
                        SliderSetting(
                            title = "Distancia mínima de aviso",
                            valueLabel = formatDistanceShort(
                                settings.minTriggerM.toDouble(),
                                settings.units
                            ),
                            value = settings.minTriggerM.toFloat(),
                            range = 40f..200f,
                            steps = 15,
                            onChange = { v ->
                                val rounded = (v / 10f).roundToInt() * 10
                                Copi.updateSettings { it.copy(minTriggerM = rounded) }
                            }
                        )
                        SwitchSetting(
                            title = "Recordatorio a 30 m",
                            subtitle = "Repite la nota justo antes de la curva.",
                            checked = settings.reminders,
                            onChange = { v -> Copi.updateSettings { it.copy(reminders = v) } }
                        )
                        SwitchSetting(
                            title = "Pitido al pasar la nota",
                            subtitle = "Un tono corto confirma que la nota ha quedado atrás.",
                            checked = settings.beepOnPass,
                            onChange = { v -> Copi.updateSettings { it.copy(beepOnPass = v) } }
                        )
                        SwitchSetting(
                            title = "Mantener pantalla encendida",
                            subtitle = "Útil como copiloto a la vista en el móvil.",
                            checked = settings.keepScreenOn,
                            onChange = { v -> Copi.updateSettings { it.copy(keepScreenOn = v) } }
                        )
                        SwitchSetting(
                            title = "Modo demo",
                            subtitle = "Simula el tramo a ~60 km/h sin GPS. Reinicia el tramo al cambiarlo.",
                            checked = settings.demoMode,
                            onChange = { Copi.setDemoMode(it) }
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Voz del copiloto", if (voiceStatus.ready) "LISTA" else "INICIANDO")
                        SwitchSetting(
                            title = "Voz activada",
                            subtitle = "Lectura de pacenotes y avisos de meta.",
                            checked = settings.voiceEnabled,
                            onChange = { Copi.toggleMute() }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Modo de voz",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.voiceMode == mode,
                                    onClick = {
                                        Copi.updateSettings { it.copy(voiceMode = mode) }
                                    },
                                    label = { Text(voiceModeLabel(mode)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = brand.primary.copy(alpha = 0.18f),
                                        selectedLabelColor = brand.primary
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when {
                                !online -> "Sin conexión: se usará la voz local."
                                !settings.onlineServices -> "Servicios online desactivados: voz local."
                                settings.elevenApiKey.isNotBlank() || settings.googleTtsApiKey.isNotBlank() ->
                                    "Voz natural en la nube disponible. Si falla, Copi usa la voz local."
                                else -> "Añade una clave de Google Cloud o ElevenLabs para una voz más natural."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        Spacer(Modifier.height(10.dp))
                        SliderSetting(
                            title = "Velocidad de habla",
                            valueLabel = String.format(Locale.US, "%.2fx", settings.speechRate),
                            value = settings.speechRate,
                            range = 0.7f..1.4f,
                            steps = 13,
                            onChange = { v -> Copi.updateSettings { it.copy(speechRate = v) } }
                        )
                        SliderSetting(
                            title = "Tono",
                            valueLabel = String.format(Locale.US, "%.2f", settings.pitch),
                            value = settings.pitch,
                            range = 0.8f..1.3f,
                            steps = 9,
                            onChange = { v -> Copi.updateSettings { it.copy(pitch = v) } }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { Copi.speakTest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.primary,
                                    contentColor = brand.onPrimary
                                )
                            ) {
                                Icon(Icons.Filled.VolumeUp, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Probar voz")
                            }
                            OutlinedButton(onClick = { showVoices = true }) {
                                Text("Voces locales")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                append("Motor: ${voiceStatus.engine ?: "—"}")
                                append(" · Voz: ${voiceStatus.voice ?: "—"}")
                                if (voiceStatus.error != null) append(" · ${voiceStatus.error}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (voiceStatus.error != null) brand.danger else brand.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent("com.android.settings.TTS_SETTINGS")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Abre Ajustes del sistema → Idiomas → Salida de texto a voz",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Text("Ajustes de voz del sistema (voces neuronales)")
                        }
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Voces en la nube")
                        SwitchSetting(
                            title = "Servicios online",
                            subtitle = "Planificación de rutas y síntesis de voz. El resto funciona offline.",
                            checked = settings.onlineServices,
                            onChange = { v -> Copi.updateSettings { it.copy(onlineServices = v) } }
                        )
                        Spacer(Modifier.height(8.dp))
                        SecretField(
                            label = "Google Cloud TTS · API key",
                            value = settings.googleTtsApiKey,
                            onChange = { v -> Copi.updateSettings { it.copy(googleTtsApiKey = v.trim()) } }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.googleVoice,
                            onValueChange = { v ->
                                Copi.updateSettings { it.copy(googleVoice = v.trim()) }
                            },
                            label = { Text("Voz Google (ej. es-ES-Neural2-F)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        SecretField(
                            label = "ElevenLabs · API key",
                            value = settings.elevenApiKey,
                            onChange = { v -> Copi.updateSettings { it.copy(elevenApiKey = v.trim()) } }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.elevenVoiceId,
                            onValueChange = { v ->
                                Copi.updateSettings { it.copy(elevenVoiceId = v.trim()) }
                            },
                            label = { Text("ElevenLabs · voice ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Las claves se guardan solo en este dispositivo. El tráfico va directamente " +
                                "a Google/ElevenLabs. En modo AUTO se prefiere ElevenLabs, luego Google y, " +
                                "si no hay red, la voz local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { Copi.precacheCurrentVoices() },
                                enabled = !precache.running
                            ) {
                                if (precache.running) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = brand.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("Precargar voces del tramo")
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                cacheBytes = 0
                                Copi.voice.clearCloudCache()
                                Toast.makeText(context, "Caché de voz borrada", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Borrar caché")
                            }
                        }
                        if (precache.running || precache.total > 0) {
                            Spacer(Modifier.height(6.dp))
                            PrecacheStatus(precache)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Caché de voz: ${formatBytes(cacheBytes)} · " +
                                "La voz local no necesita conexión; la nube solo para frases ya sintetizadas.",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Avanzado")
                        OutlinedTextField(
                            value = settings.osrmBaseUrl,
                            onValueChange = { v -> Copi.updateSettings { it.copy(osrmBaseUrl = v.trim()) } },
                            label = { Text("Servidor OSRM") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.nominatimBaseUrl,
                            onValueChange = { v -> Copi.updateSettings { it.copy(nominatimBaseUrl = v.trim()) } },
                            label = { Text("Servidor Nominatim") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Los servidores públicos de demo tienen límites de uso. Para un uso intensivo, " +
                                "instala tu propia instancia de OSRM/Nominatim y cámbiala aquí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item {
                CopiCard {
                    Column {
                        SectionTitle("Aplicación")
                        ActionRow(
                            icon = Icons.Filled.MenuBook,
                            title = "Manual rápido",
                            subtitle = "Primeros pasos, GPX, voz y Android Auto"
                        ) { onOpenHelp() }
                        HorizontalDivider(color = brand.line)
                        ActionRow(
                            icon = Icons.Filled.Info,
                            title = "Acerca de Copi",
                            subtitle = "Edición, autoría, licencia y avisos legales"
                        ) { onOpenAbout() }
                        HorizontalDivider(color = brand.line)
                        ActionRow(
                            icon = Icons.Filled.Settings,
                            title = "Restablecer ajustes",
                            subtitle = "Conserva tus tramos guardados"
                        ) { showReset = true }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Versión ${BuildConfig.VERSION_NAME} · ${BuildConfig.FLAVOR} · " +
                                "datos © OpenStreetMap contributors",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.muted
                        )
                    }
                }
            }

            item { BrandFooter() }
        }
    }

    if (showVoices) {
        VoicePickerDialog(
            selectedName = settings.selectedVoice,
            onRefresh = {
                Copi.voice.refreshVoices()
                Copi.voice.availableVoices()
            },
            onSelect = {
                Copi.selectVoice(it)
                showVoices = false
            },
            onDismiss = { showVoices = false }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Restablecer ajustes") },
            text = { Text("Se restablecerán unidades, voz, claves y preferencias. Tus tramos no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    Copi.resetSettings()
                    Toast.makeText(context, "Ajustes restablecidos", Toast.LENGTH_SHORT).show()
                }) { Text("Restablecer") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PrecacheStatus(progress: PrecacheProgress) {
    val brand = LocalBrand.current
    Column {
        if (progress.total > 0) {
            LinearProgressIndicator(
                progress = { progress.done.toFloat() / progress.total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = brand.primary,
                trackColor = brand.line
            )
            Spacer(Modifier.height(4.dp))
            Text(
                progress.message.ifBlank {
                    "Sintetizando ${progress.done}/${progress.total}" +
                        if (progress.failed > 0) " · ${progress.failed} fallos" else ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted
            )
        } else if (progress.message.isNotBlank()) {
            Text(
                progress.message,
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted
            )
        }
    }
}

@Composable
private fun VoicePickerDialog(
    selectedName: String?,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val brand = LocalBrand.current
    var voices by remember { mutableStateOf(Copi.voice.availableVoices()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voces locales") },
        text = {
            Column {
                Text(
                    "Se prefieren voces neuronales instaladas en español. Para más voces, instala Google " +
                        "Speech Services y descarga el paquete de español.",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.muted
                )
                Spacer(Modifier.height(8.dp))
                if (voices.isEmpty()) {
                    Text("Cargando voces…", color = brand.muted)
                } else {
                    Column(
                        Modifier
                            .height(320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        voices.forEach { voice ->
                            VoiceRow(
                                voice = voice,
                                selected = voice.name == selectedName || (selectedName == null && voice.selected),
                                onClick = { onSelect(voice.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onRefresh()
                voices = Copi.voice.availableVoices()
            }) { Text("Actualizar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun VoiceRow(voice: VoiceOption, selected: Boolean, onClick: () -> Unit) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(
                voice.name,
                style = MaterialTheme.typography.bodySmall,
                color = brand.text,
                maxLines = 1
            )
            Text(
                buildString {
                    append(voice.locale)
                    append(" · calidad ")
                    append(voice.quality)
                    if (voice.networkRequired) append(" · requiere red")
                },
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted
            )
        }
    }
}

@Composable
private fun OptionChips(
    label: String,
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit
) {
    val brand = LocalBrand.current
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = brand.muted,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, (text, selected) ->
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(index) },
                    label = { Text(text) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = brand.primary.copy(alpha = 0.18f),
                        selectedLabelColor = brand.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = brand.text, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = brand.muted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = brand.onPrimary,
                checkedTrackColor = brand.primary,
                uncheckedThumbColor = brand.muted,
                uncheckedTrackColor = brand.line
            )
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    val brand = LocalBrand.current
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(vertical = 3.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = brand.text)
            Spacer(Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = brand.accent)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = brand.primary,
                activeTrackColor = brand.primary,
                inactiveTrackColor = brand.line
            )
        )
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar" else "Mostrar"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = brand.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = brand.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = brand.muted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = brand.muted, modifier = Modifier.size(18.dp))
    }
}

private fun voiceModeLabel(mode: VoiceMode): String = when (mode) {
    VoiceMode.AUTO -> "Auto"
    VoiceMode.LOCAL -> "Local"
    VoiceMode.GOOGLE -> "Google"
    VoiceMode.ELEVEN -> "ElevenLabs"
}
