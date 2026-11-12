package com.silicio.copi.nav

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.VoiceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

data class VoiceOption(
    val name: String,
    val locale: String,
    val quality: Int,
    val networkRequired: Boolean,
    val selected: Boolean
)

data class VoiceStatus(
    val ready: Boolean = false,
    val engine: String? = null,
    val voice: String? = null,
    val error: String? = null,
    val speaking: Boolean = false
)

data class PrecacheProgress(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val message: String = ""
)

/**
 * Voz del copiloto. Orden de preferencia:
 *  1) Voz neuronal en la nube con caché (ElevenLabs o Google Cloud TTS).
 *  2) Mejor voz local instalada (prefiere voces neuronales de alta calidad).
 *  3) Motor TTS del sistema.
 * Nunca bloquea el motor del tramo si falla la red.
 */
class VoiceGuidance(
    context: Context,
    private val settingsProvider: () -> AppSettings,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val cloud = CloudTts(appContext)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingLocal: String? = null
    private var voiceOptions: List<VoiceOption> = emptyList()
    private var counter = 0

    private val queue = Channel<Utterance>(Channel.UNLIMITED)
    private var consumerStarted = false
    private var player: MediaPlayer? = null
    private var playContinuation: kotlinx.coroutines.CancellableContinuation<Boolean?>? = null
    @Volatile
    private var generation = 0

    private data class Utterance(val text: String, val generation: Int)

    private val focusRequest: AudioFocusRequest? = audioManager?.let { manager ->
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setWillPauseWhenDucked(false)
            .build()
    }

    private val _status = MutableStateFlow(VoiceStatus())
    val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    private val _precache = MutableStateFlow(PrecacheProgress())
    val precache: StateFlow<PrecacheProgress> = _precache.asStateFlow()

    private var precacheJob: Job? = null

    init {
        initialize()
        startConsumer()
    }

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { code ->
            if (code == TextToSpeech.SUCCESS) {
                ttsReady = true
                configureTts()
                pendingLocal?.let { text ->
                    pendingLocal = null
                    localSpeak(text, settingsProvider())
                }
            } else {
                _status.update {
                    it.copy(error = "No se pudo iniciar el motor de voz del sistema")
                }
            }
        }
    }

    private fun configureTts() {
        val engine = tts ?: return
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        val voices = engine.voices?.toList().orEmpty()
        val spanish = voices.filter { it.locale.language == "es" }
        val pool = spanish.ifEmpty { voices }
        val selectedName = settingsProvider().selectedVoice
        val selected = selectedName?.let { name -> voices.firstOrNull { it.name == name } }
        val best = selected ?: pool.maxByOrNull { scoreVoice(it) }
        if (best != null) {
            runCatching { engine.voice = best }
        } else {
            runCatching { engine.language = Locale("es", "ES") }
        }
        voiceOptions = voices.map { voice ->
            VoiceOption(
                name = voice.name,
                locale = voice.locale.toLanguageTag(),
                quality = voice.quality,
                networkRequired = voice.isNetworkConnectionRequired,
                selected = voice.name == best?.name
            )
        }.sortedWith(compareByDescending<VoiceOption> { it.selected }
            .thenByDescending { it.locale.startsWith("es") }
            .thenByDescending { it.quality })

        _status.value = VoiceStatus(
            ready = true,
            engine = engine.defaultEngine,
            voice = best?.name,
            error = if (spanish.isEmpty()) "No hay voces en español instaladas" else null
        )
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _status.update { it.copy(speaking = true) }
            }

            override fun onDone(utteranceId: String?) {
                _status.update { it.copy(speaking = false) }
                abandonFocus()
            }

            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {
                onDone(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _status.update { it.copy(speaking = false) }
                abandonFocus()
            }
        })
    }

    private fun scoreVoice(voice: Voice): Int {
        var score = voice.quality
        if (!voice.isNetworkConnectionRequired) score += 120
        val name = voice.name.lowercase(Locale.ROOT)
        if (name.contains("neural") || name.contains("wavenet")) score += 220
        if (voice.locale.language == "es") score += 500
        if (voice.locale.country.equals("ES", true)) score += 80
        return score
    }

    fun availableVoices(): List<VoiceOption> {
        initialize()
        return voiceOptions
    }

    fun refreshVoices() {
        if (ttsReady) configureTts()
    }

    fun speak(text: String, interrupt: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        initialize()
        if (!settingsProvider().voiceEnabled) return
        if (interrupt) {
            generation++
            interruptCurrent()
        }
        queue.trySend(Utterance(clean, generation))
    }

    fun test() {
        speak(
            "Copi, tu copiloto de rally. Voz configurada y lista para cantar el tramo.",
            interrupt = true
        )
    }

    fun stop() {
        generation++
        interruptCurrent()
        _status.update { it.copy(speaking = false) }
    }

    private fun interruptCurrent() {
        while (queue.tryReceive().isSuccess) Unit
        runCatching { tts?.stop() }
        playContinuation?.let { cont ->
            if (cont.isActive) cont.resume(null)
        }
        playContinuation = null
        releasePlayer()
        abandonFocus()
    }

    private fun startConsumer() {
        if (consumerStarted) return
        consumerStarted = true
        scope.launch {
            for (utterance in queue) {
                runCatching { playOne(utterance) }
            }
        }
    }

    private suspend fun playOne(utterance: Utterance) {
        if (utterance.generation != generation) return
        val settings = settingsProvider()
        if (!settings.voiceEnabled) return
        val speaker = resolveSpeaker(settings)
        if (speaker == Speaker.LOCAL) {
            localSpeak(utterance.text, settings)
            return
        }
        _status.update { it.copy(speaking = true) }
        val file = runCatching { cloud.synthesize(utterance.text, settings, speaker) }.getOrNull()
        if (utterance.generation != generation) return
        if (file == null) {
            _status.update {
                it.copy(
                    speaking = false,
                    error = "Voz en la nube no disponible. Usando voz local."
                )
            }
            localSpeak(utterance.text, settings)
            return
        }
        val played = playFile(file)
        if (played == false && utterance.generation == generation) {
            _status.update {
                it.copy(error = "No se pudo reproducir la voz en la nube. Usando voz local.")
            }
            localSpeak(utterance.text, settings)
        }
    }

    private fun resolveSpeaker(settings: AppSettings): Speaker {
        if (!settings.onlineServices) return Speaker.LOCAL
        val online = isOnline(appContext)
        return when (settings.voiceMode) {
            VoiceMode.LOCAL -> Speaker.LOCAL
            VoiceMode.GOOGLE -> if (online && settings.googleTtsApiKey.isNotBlank()) Speaker.GOOGLE else Speaker.LOCAL
            VoiceMode.ELEVEN -> if (online && settings.elevenApiKey.isNotBlank()) Speaker.ELEVEN else Speaker.LOCAL
            VoiceMode.AUTO -> when {
                online && settings.elevenApiKey.isNotBlank() -> Speaker.ELEVEN
                online && settings.googleTtsApiKey.isNotBlank() -> Speaker.GOOGLE
                else -> Speaker.LOCAL
            }
        }
    }

    private fun localSpeak(text: String, settings: AppSettings) {
        val engine = tts
        if (engine == null || !ttsReady) {
            pendingLocal = text
            initialize()
            return
        }
        engine.setSpeechRate(settings.speechRate.coerceIn(0.5f, 1.6f))
        engine.setPitch(settings.pitch.coerceIn(0.5f, 1.6f))
        requestFocus()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = engine.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            params,
            "copi-${counter++}"
        )
        if (result != TextToSpeech.SUCCESS) {
            _status.update { it.copy(error = "El motor TTS rechazó la locución") }
            abandonFocus()
        }
    }

    private suspend fun playFile(file: File): Boolean? = withContext(Dispatchers.Main) {
        val mp = MediaPlayer()
        return@withContext try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            requestFocus()
            player = mp
            suspendCancellableCoroutine<Boolean?> { cont ->
                playContinuation = cont
                mp.setOnCompletionListener {
                    playContinuation = null
                    releasePlayer(it)
                    if (cont.isActive) cont.resume(true)
                    abandonFocus()
                }
                mp.setOnErrorListener { _, _, _ ->
                    playContinuation = null
                    releasePlayer(mp)
                    if (cont.isActive) cont.resume(false)
                    abandonFocus()
                    true
                }
                cont.invokeOnCancellation {
                    playContinuation = null
                    releasePlayer(mp)
                }
                mp.start()
            }
        } catch (t: Throwable) {
            releasePlayer(mp)
            abandonFocus()
            _status.update { it.copy(speaking = false) }
            false
        }
    }

    private fun releasePlayer(target: MediaPlayer? = player) {
        runCatching { target?.stop() }
        runCatching { target?.release() }
        if (player === target) player = null
        _status.update { it.copy(speaking = false) }
    }

    private fun requestFocus() {
        runCatching { audioManager?.requestAudioFocus(focusRequest!!) }
    }

    private fun abandonFocus() {
        runCatching { audioManager?.abandonAudioFocusRequest(focusRequest!!) }
    }

    fun precachePhrases(phrases: List<String>): Boolean {
        if (precacheJob?.isActive == true) return false
        val unique = phrases.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (unique.isEmpty()) return false
        val settings = settingsProvider()
        val speaker = resolveSpeaker(settings)
        if (speaker == Speaker.LOCAL) {
            _precache.value = PrecacheProgress(
                message = "Configura una clave de voz en la nube para precargar."
            )
            return false
        }
        precacheJob = scope.launch {
            var done = 0
            var failed = 0
            _precache.value = PrecacheProgress(running = true, total = unique.size)
            for (text in unique) {
                val file = runCatching { cloud.synthesize(text, settingsProvider(), speaker) }.getOrNull()
                if (file == null) failed++
                done++
                _precache.value = PrecacheProgress(
                    running = true,
                    done = done,
                    total = unique.size,
                    failed = failed
                )
            }
            _precache.value = PrecacheProgress(
                running = false,
                done = done,
                total = unique.size,
                failed = failed,
                message = if (failed == 0) "Voces precargadas para uso sin conexión" else "$failed frases fallaron"
            )
        }
        return true
    }

    fun cloudCacheBytes(): Long = cloud.cacheSizeBytes()

    fun clearCloudCache(): Int = cloud.clearCache()

    fun shutdown() {
        interruptCurrent()
        runCatching { tts?.shutdown() }
        tts = null
        ttsReady = false
    }
}
