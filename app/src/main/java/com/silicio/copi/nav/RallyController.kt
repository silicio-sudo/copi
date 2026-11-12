package com.silicio.copi.nav

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.OpenableColumns
import com.silicio.copi.core.RoutePath
import com.silicio.copi.core.SettingsStore
import com.silicio.copi.core.formatDistanceSpoken
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.GeoPoint
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.PacenoteGenerator
import com.silicio.copi.data.Roadbook
import com.silicio.copi.data.RoadbookRepository
import com.silicio.copi.data.RoutePlanner
import com.silicio.copi.data.announcement
import com.silicio.copi.data.phraseEs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

/**
 * Fachada única de la aplicación. Mantiene el motor, la voz, el GPS, la
 * biblioteca de tramos y el servicio en primer plano coordinados.
 */
object Copi {
    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: RoadbookRepository
        private set
    lateinit var voice: VoiceGuidance
        private set
    lateinit var engine: RallyEngine
        private set

    private lateinit var location: LocationProvider
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _roadbooks = MutableStateFlow<List<Roadbook>>(emptyList())
    val roadbooks: StateFlow<List<Roadbook>> = _roadbooks.asStateFlow()

    private val _activeRoadbook = MutableStateFlow<Roadbook?>(null)
    val activeRoadbook: StateFlow<Roadbook?> = _activeRoadbook.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _pendingDestination = MutableStateFlow<String?>(null)
    val pendingDestination: StateFlow<String?> = _pendingDestination.asStateFlow()

    var lastFix: LocationFix? = null
        private set

    private var ticker: Job? = null
    private var lastFinishedState = false

    fun init(app: Application) {
        if (::engine.isInitialized) return
        context = app.applicationContext
        settings = SettingsStore(context)
        repository = RoadbookRepository(context)
        voice = VoiceGuidance(context, { settings.current }, scope)
        engine = RallyEngine(
            settings = { settings.current },
            speak = { text, interrupt ->
                if (settings.current.voiceEnabled) voice.speak(text, interrupt)
            },
            beep = { beep() }
        )
        location = LocationProvider(context) { fix ->
            lastFix = fix
            engine.onLocation(fix)
        }
        scope.launch {
            engine.state.collect { state ->
                if (state.finished && !lastFinishedState) {
                    lastFinishedState = true
                    stopInfrastructure()
                }
                if (!state.finished) lastFinishedState = false
            }
        }
        bootstrap()
    }

    private fun bootstrap() {
        scope.launch {
            runCatching {
                var list = repository.list()
                if (list.isEmpty()) {
                    repository.seedDemoIfEmpty()
                    list = repository.list()
                }
                _roadbooks.value = list
                val active = list.firstOrNull { it.id == settings.current.activeRoadbookId }
                    ?: list.firstOrNull()
                active?.let { activate(it) }
            }.onFailure { _lastError.value = "No se pudieron cargar los tramos: ${it.message}" }
        }
    }

    fun refreshRoadbooks() {
        scope.launch {
            runCatching { _roadbooks.value = repository.list() }
                .onFailure { _lastError.value = it.message }
        }
    }

    fun activate(roadbook: Roadbook) {
        settings.update { it.copy(activeRoadbookId = roadbook.id) }
        _activeRoadbook.value = roadbook
        engine.setRoadbook(roadbook)
        stopInfrastructure()
    }

    fun startNavigation() {
        val rb = engine.state.value.roadbook ?: _activeRoadbook.value
        if (rb == null) {
            _lastError.value = "Selecciona un tramo antes de iniciar"
            return
        }
        val demo = settings.current.demoMode || !LocationProvider.hasPermission(context)
        if (!settings.current.demoMode && !LocationProvider.hasPermission(context)) {
            _lastError.value = "Sin permiso de ubicación. Activa el modo demo o concede el permiso GPS."
            return
        }
        if (engine.state.value.roadbook?.id != rb.id) engine.setRoadbook(rb)
        _lastError.value = null
        _message.value = null
        if (demo) stopLocationAndService()
        engine.start(demo)
        startTicker()
        if (!demo) {
            location.start()
            startService()
        }
    }

    fun pauseNavigation() {
        engine.pause()
        stopTicker()
        if (!engine.state.value.demo) location.stop()
        syncService()
    }

    fun resumeNavigation() {
        if (engine.state.value.finished) {
            _lastError.value = "El tramo ha terminado. Reinícialo para volver a empezar."
            return
        }
        engine.resume()
        startTicker()
        if (!engine.state.value.demo) location.start()
        syncService()
    }

    fun togglePause() {
        val state = engine.state.value
        if (state.paused) resumeNavigation() else if (state.running) pauseNavigation() else startNavigation()
    }

    fun resetNavigation() {
        engine.reset()
        stopInfrastructure()
        voice.stop()
    }

    fun skipNext() = engine.skipNext()

    fun previousNote() = engine.previousNote()

    fun selectNote(index: Int) = engine.selectNote(index)

    fun recalibrate() {
        engine.recalibrate(lastFix ?: location.bestLastKnown())
    }

    fun toggleMute() {
        val enabled = !settings.current.voiceEnabled
        settings.update { it.copy(voiceEnabled = enabled) }
        if (!enabled) voice.stop()
        engine.refresh()
        syncService()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
        engine.refresh()
        syncService()
    }

    fun selectVoice(name: String) {
        settings.update { it.copy(selectedVoice = name) }
        voice.refreshVoices()
        engine.refresh()
    }

    fun resetSettings() {
        val active = settings.current.activeRoadbookId
        settings.replace(AppSettings(activeRoadbookId = active))
        voice.stop()
        voice.refreshVoices()
        engine.refresh()
        syncService()
    }

    fun setDemoMode(enabled: Boolean) {
        settings.update { it.copy(demoMode = enabled) }
        if (engine.state.value.running || engine.state.value.paused) {
            engine.stop()
            resetNavigation()
            _message.value = "Modo demo ${if (enabled) "activado" else "desactivado"}. Reinicia el tramo."
        }
    }

    fun speakTest() = voice.test()

    fun importGpx(uri: Uri) {
        scope.launch {
            _importing.value = true
            _lastError.value = null
            runCatching {
                val name = displayName(uri) ?: "Tramo GPX"
                val roadbook = context.contentResolver.openInputStream(uri)?.use { stream ->
                    repository.importGpx(name, stream)
                } ?: error("No se pudo abrir el archivo")
                afterImport(roadbook)
            }.onFailure { _lastError.value = "Error al importar GPX: ${it.message}" }
            _importing.value = false
        }
    }

    fun importCsv(uri: Uri) {
        scope.launch {
            _importing.value = true
            _lastError.value = null
            runCatching {
                val name = displayName(uri)?.substringBeforeLast('.') ?: "Tramo CSV"
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("No se pudo abrir el archivo")
                val roadbook = repository.importCsv(name, text)
                afterImport(roadbook)
            }.onFailure { _lastError.value = "Error al importar CSV: ${it.message}" }
            _importing.value = false
        }
    }

    fun deleteRoadbook(id: String) {
        scope.launch {
            repository.delete(id)
            val list = repository.list()
            _roadbooks.value = list
            if (_activeRoadbook.value?.id == id || settings.current.activeRoadbookId == id) {
                val next = list.firstOrNull()
                if (next != null) activate(next) else {
                    _activeRoadbook.value = null
                    engine.setRoadbook(null)
                    settings.update { it.copy(activeRoadbookId = null) }
                }
            }
        }
    }

    fun exportCsv(roadbook: Roadbook): String = repository.exportCsv(roadbook)

    fun shareCsv(roadbook: Roadbook) {
        val csv = repository.exportCsv(roadbook)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "${roadbook.name}.csv")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        context.startActivity(
            Intent.createChooser(send, "Compartir roadbook").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun planRoute(destinationQuery: String, originQuery: String? = null): Roadbook {
        if (!settings.current.onlineServices) error("Los servicios online están desactivados en Ajustes")
        if (!isOnline(context)) error("No hay conexión a internet")
        val planner = RoutePlanner { settings.current }
        val origin = resolveOrigin(planner, originQuery)
        val destination = RoutePlanner.parseCoordinates(destinationQuery)
            ?: planner.geocode(destinationQuery).firstOrNull()
            ?: error("No se encontró el destino")
        val planned = planner.route(origin, GeoPoint(destination.lat, destination.lon))
        val roadbook = PacenoteGenerator.fromRoute(
            points = planned.points,
            name = "Ruta a ${destination.name.take(50)}",
            source = "osrm"
        )
        val saved = repository.save(roadbook)
        _roadbooks.value = repository.list()
        return saved
    }

    private suspend fun resolveOrigin(planner: RoutePlanner, originQuery: String?): GeoPoint {
        if (!originQuery.isNullOrBlank()) {
            val place = RoutePlanner.parseCoordinates(originQuery)
                ?: planner.geocode(originQuery).firstOrNull()
                ?: error("No se encontró el origen")
            return GeoPoint(place.lat, place.lon)
        }
        lastFix?.let { return GeoPoint(it.lat, it.lon) }
        location.bestLastKnown()?.let {
            lastFix = it
            return GeoPoint(it.lat, it.lon)
        }
        _activeRoadbook.value?.route?.firstOrNull()?.let { return it }
        error("No hay posición GPS. Activa el modo demo o indica un origen.")
    }

    fun setPendingDestination(query: String?) {
        _pendingDestination.value = query
    }

    fun handleNavigationIntent(query: String) {
        scope.launch {
            runCatching { planRoute(query) }
                .onSuccess { roadbook ->
                    activate(roadbook)
                    _message.value = "Tramo listo: ${roadbook.name}"
                    if (settings.current.voiceEnabled) {
                        voice.speak("Tramo listo. ${roadbook.name}. Pulsa iniciar.", true)
                    }
                }
                .onFailure { _lastError.value = "No se pudo preparar el destino: ${it.message}" }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearError() {
        _lastError.value = null
    }

    fun precacheCurrentVoices() {
        val roadbook = engine.state.value.roadbook ?: _activeRoadbook.value ?: return
        val units = settings.current.units
        val phrases = buildList {
            roadbook.sortedNotes.filter { it.type != com.silicio.copi.data.NoteType.START }.forEach { note ->
                add(note.phraseEs())
                add("Ahora, ${note.phraseEs()}")
                add(note.announcement(100.0, units))
            }
            add("Copi listo. ${roadbook.name}.")
            add("Meta. Tramo completado.")
        }
        voice.precachePhrases(phrases)
    }

    fun onlineNow(): Boolean = isOnline(context)

    fun gpsAvailable(): Boolean = LocationProvider.hasPermission(context)

    private suspend fun afterImport(roadbook: Roadbook) {
        _roadbooks.value = repository.list()
        activate(roadbook)
        _message.value = "Tramo importado: ${roadbook.name}"
    }

    private fun displayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (true) {
                delay(250)
                engine.tick()
                if (!engine.state.value.running) break
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun startService() {
        if (settings.current.demoMode) return
        runCatching {
            val intent = Intent(context, RallyService::class.java).setAction(RallyService.ACTION_SYNC)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }.onFailure {
            _lastError.value = "No se pudo iniciar el servicio en primer plano: ${it.message}"
        }
    }

    private fun syncService() {
        if (!engine.state.value.running && !engine.state.value.paused) {
            context.stopService(Intent(context, RallyService::class.java))
            return
        }
        if (engine.state.value.demo) return
        startService()
    }

    private fun stopInfrastructure() {
        stopTicker()
        runCatching { location.stop() }
        if (::context.isInitialized) {
            runCatching { context.stopService(Intent(context, RallyService::class.java)) }
        }
        lastFinishedState = false
    }

    private fun stopLocationAndService() {
        runCatching { location.stop() }
        runCatching { context.stopService(Intent(context, RallyService::class.java)) }
    }

    private fun beep() {
        if (!settings.current.beepOnPass) return
        scope.launch {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
                delay(220)
                tone.release()
            }
        }
    }

    fun shutdown() {
        stopInfrastructure()
        voice.shutdown()
    }
}
