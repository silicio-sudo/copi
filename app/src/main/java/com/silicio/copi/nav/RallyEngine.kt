package com.silicio.copi.nav

import android.os.SystemClock
import com.silicio.copi.core.RoutePath
import com.silicio.copi.core.bearingDeg
import com.silicio.copi.core.distanceM
import com.silicio.copi.core.formatDurationLong
import com.silicio.copi.core.formatDistanceSpoken
import com.silicio.copi.core.offsetPoint
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.GeoPoint
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.Roadbook
import com.silicio.copi.data.announcement
import com.silicio.copi.data.phraseEs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.sin

data class RallyState(
    val roadbook: Roadbook? = null,
    val running: Boolean = false,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val speedKmh: Float = 0f,
    val headingDeg: Float = 0f,
    val gpsFix: Boolean = false,
    val accuracyM: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stageDistanceM: Double = 0.0,
    val elapsedMs: Long = 0L,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val nextIndex: Int = 0,
    val distanceToNextM: Double? = null,
    val nextNote: PaceNote? = null,
    val upcoming: List<PaceNote> = emptyList(),
    val trail: List<GeoPoint> = emptyList(),
    val demo: Boolean = false,
    val voiceEnabled: Boolean = true,
    val progress: Float = 0f,
    val message: String = ""
) {
    val totalNotes: Int get() = roadbook?.notes?.size ?: 0
    val noteNumber: Int
        get() = if (totalNotes == 0) 0 else (nextIndex + 1).coerceAtMost(totalNotes)
}

/**
 * Motor del tramo: acumula distancia GPS, avanza notas, decide cuándo hablar
 * y produce un estado observable para móvil, notificación y Android Auto.
 * No conoce Android salvo el reloj: se puede probar con datos sintéticos.
 */
class RallyEngine(
    private val settings: () -> AppSettings,
    private val speak: (String, Boolean) -> Unit,
    private val beep: () -> Unit
) {
    private val _state = MutableStateFlow(RallyState())
    val state: StateFlow<RallyState> = _state.asStateFlow()

    private var roadbook: Roadbook? = null
    private var notes: List<PaceNote> = emptyList()
    private var route: RoutePath = RoutePath(emptyList())

    private var nextIndex = 0
    private var rawDistanceM = 0.0
    private var offsetM = 0.0
    private var smoothedSpeedMps = 0.0
    private var headingDeg = 0.0
    private var maxSpeedKmh = 0f
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var lastFix: LocationFix? = null
    private var lastFixAt = 0L

    private var running = false
    private var paused = false
    private var finished = false
    private var demoMode = false

    private var startedAt = 0L
    private var pauseAccum = 0L
    private var pausedAt = 0L
    private var frozenElapsed = 0L

    private var calledIndex = -1
    private var reminderIndex = -1
    private var beepedIndex = -1

    private var demoPoint: GeoPoint? = null
    private var demoHeading = 0.0
    private var demoLastTick = 0L
    private var demoSpeed = 0.0
    private var demoDistance = 0.0

    private val trail = ArrayDeque<GeoPoint>()

    fun setRoadbook(newRoadbook: Roadbook?) {
        stop()
        roadbook = newRoadbook
        notes = newRoadbook?.sortedNotes.orEmpty()
        route = RoutePath(newRoadbook?.route.orEmpty())
        resetProgress()
        emit()
    }

    fun start(demo: Boolean = settings().demoMode) {
        val rb = roadbook ?: return
        val now = SystemClock.elapsedRealtime()
        resetProgress()
        running = true
        paused = false
        finished = false
        demoMode = demo
        startedAt = now
        pauseAccum = 0L
        pausedAt = 0L
        frozenElapsed = 0L
        demoLastTick = now
        demoDistance = 0.0
        demoSpeed = 0.0
        demoPoint = route.points.firstOrNull() ?: lastFix?.let { GeoPoint(it.lat, it.lon) }
            ?: GeoPoint(40.8431, -3.9487)
        demoHeading = route.pointAt(0.0)?.second ?: 0.0
        lastFix = null
        lastFixAt = 0L

        emit()

        if (demo) {
            val first = nextNote()
            val intro = buildString {
                append("Copi listo. ")
                append(rb.name)
                append(". ")
                if (first != null) append(first.announcement(first.distanceM, settings().units))
            }
            if (settings().voiceEnabled) speak(intro, true)
            calledIndex = if (first != null) nextIndex else -1
        } else if (settings().voiceEnabled) {
            speak("Copi listo. ${rb.name}. Esperando señal GPS.", true)
        }
    }

    fun pause() {
        if (!running || paused) return
        frozenElapsed = elapsedMs()
        paused = true
        pausedAt = SystemClock.elapsedRealtime()
        emit()
    }

    fun resume() {
        if (!running || !paused) return
        pauseAccum += SystemClock.elapsedRealtime() - pausedAt
        paused = false
        emit()
    }

    fun stop() {
        if (running) {
            frozenElapsed = elapsedMs()
        }
        running = false
        paused = false
        emit()
    }

    fun reset() {
        stop()
        resetProgress()
        emit()
    }

    /** Vuelve a emitir el estado (por ejemplo tras cambiar ajustes). */
    fun refresh() = emit()

    fun skipNext() {
        if (notes.isEmpty()) return
        val maxIndex = notes.indexOfLast { it.type != NoteType.FINISH }
        if (nextIndex < maxIndex) {
            nextIndex++
            resetCalloutFlags()
            emit()
        }
    }

    fun previousNote() {
        if (notes.isEmpty()) return
        val firstCorner = notes.indexOfFirst { it.type != NoteType.START }
        if (nextIndex > max(firstCorner, 0)) {
            nextIndex--
            resetCalloutFlags()
            emit()
        }
    }

    fun selectNote(index: Int) {
        if (index !in notes.indices) return
        nextIndex = index
        resetCalloutFlags()
        emit()
    }

    /**
     * Ajusta el origen del tramo al punto GPS actual. Si el roadbook trae
     * trazado, encaja el punto en la ruta; si no, coloca la siguiente nota a
     * 60 m de distancia.
     */
    fun recalibrate(fix: LocationFix?) {
        if (roadbook == null) return
        if (route.isUsable && fix != null) {
            val nearest = route.nearest(fix.lat, fix.lon)
            if (nearest != null) {
                offsetM = nearest.first - rawDistanceM
                val effective = effectiveDistance()
                nextIndex = firstNoteAtOrAfter(effective)
                resetCalloutFlags()
                emit()
                speak("Alineado en el tramo. ${nearest.second.roundInt()} metros de desvío.", true)
                return
            }
        }
        val target = notes.getOrNull(nextIndex) ?: return
        offsetM = (target.distanceM - 60.0) - rawDistanceM
        resetCalloutFlags()
        emit()
        speak("Recalibrado. Siguiente nota en 60 metros.", true)
    }

    fun onLocation(fix: LocationFix) {
        if (!running || paused || demoMode) return
        val now = SystemClock.elapsedRealtime()
        val previous = lastFix
        val dt = if (previous == null) 0.0 else ((fix.timeMs - previous.timeMs) / 1000.0).coerceIn(0.1, 8.0)
        val moved = if (previous == null) 0.0 else distanceM(
            GeoPoint(previous.lat, previous.lon),
            GeoPoint(fix.lat, fix.lon)
        )

        if (previous != null && moved in 0.4..250.0 && fix.accuracyM <= 80f) {
            rawDistanceM += moved
            addTrail(GeoPoint(fix.lat, fix.lon))
        }

        val derivedSpeed = if (dt > 0.2 && moved in 0.05..250.0) moved / dt else 0.0
        val reported = if (fix.speedMps >= 0f && fix.accuracyM < 60f) fix.speedMps.toDouble() else derivedSpeed
        smoothedSpeedMps = if (smoothedSpeedMps <= 0.1) reported else smoothedSpeedMps * 0.55 + reported * 0.45

        if (fix.bearingDeg >= 0f && smoothedSpeedMps > 1.0) {
            headingDeg = fix.bearingDeg.toDouble()
        } else if (previous != null && moved > 2.0) {
            headingDeg = bearingDeg(GeoPoint(previous.lat, previous.lon), GeoPoint(fix.lat, fix.lon))
        }
        val kmh = (smoothedSpeedMps * 3.6).toFloat()
        if (kmh > maxSpeedKmh) maxSpeedKmh = kmh

        latitude = fix.lat
        longitude = fix.lon
        lastFix = fix
        lastFixAt = now

        evaluate(now)
        emit()
    }

    fun tick(now: Long = SystemClock.elapsedRealtime()) {        if (!running || paused) {
            if (running && paused) emit(now)
            return
        }
        if (demoMode) {
            val dt = ((now - demoLastTick).coerceIn(0L, 2000L)) / 1000.0
            demoLastTick = now
            if (dt > 0.05) tickDemo(dt)
        } else {
            if (lastFixAt > 0 && now - lastFixAt > 5_000) {
                smoothedSpeedMps *= 0.8
                if (now - lastFixAt > 9_000) smoothedSpeedMps = 0.0
            }
            updatePositionState()
        }
        evaluate(now)
        emit(now)
    }

    private fun tickDemo(dt: Double) {
        if (demoPoint == null) demoPoint = GeoPoint(40.8431, -3.9487)
        val elapsedS = elapsedMs() / 1000.0
        val target = 16.5 + 3.2 * sin(elapsedS / 7.0)
        demoSpeed += (target - demoSpeed) * min(1.0, dt * 1.6)
        demoDistance += demoSpeed * dt
        rawDistanceM = demoDistance
        val path = route.pointAt(effectiveDistance())
        if (path != null) {
            demoPoint = path.first
            demoHeading = path.second
        } else {
            demoHeading += dt * 3.0
            demoPoint = offsetPoint(demoPoint!!, demoHeading, demoSpeed * dt)
        }
        smoothedSpeedMps = demoSpeed
        headingDeg = demoHeading
        latitude = demoPoint?.lat
        longitude = demoPoint?.lon
        demoPoint?.let { addTrail(it) }
        lastFixAt = SystemClock.elapsedRealtime()
        val kmh = (demoSpeed * 3.6).toFloat()
        if (kmh > maxSpeedKmh) maxSpeedKmh = kmh
    }

    private fun evaluate(now: Long) {
        val total = roadbook?.distanceM ?: return
        val effective = effectiveDistance()
        updatePositionState()

        if (effective >= total - 0.5) {
            finish(now)
            return
        }

        while (nextIndex < notes.size && effective >= notes[nextIndex].distanceM + 1.5) {
            val note = notes[nextIndex]
            if (note.type != NoteType.START && note.type != NoteType.FINISH &&
                settings().beepOnPass && beepedIndex != nextIndex
            ) {
                beep()
                beepedIndex = nextIndex
            }
            nextIndex++
            reminderIndex = -1
        }

        val note = nextNote() ?: return
        val distance = (note.distanceM - effective).coerceAtLeast(0.0)
        val trigger = max(
            settings().minTriggerM.toDouble(),
            smoothedSpeedMps * settings().lookaheadSeconds
        ).coerceIn(40.0, 700.0)

        if (distance <= trigger && calledIndex != nextIndex && settings().voiceEnabled) {
            calledIndex = nextIndex
            speak(note.announcement(distance, settings().units), true)
        }

        if (settings().reminders && distance <= 35.0 && distance > 4.0 &&
            reminderIndex != nextIndex && calledIndex == nextIndex && settings().voiceEnabled
        ) {
            reminderIndex = nextIndex
            speak(note.phraseEs(), false)
        }
    }

    private fun finish(now: Long) {
        if (finished) return
        frozenElapsed = elapsedMs(now)
        finished = true
        running = false
        paused = false
        smoothedSpeedMps = 0.0
        nextIndex = notes.size
        emit(now)
        if (settings().voiceEnabled) {
            speak("Meta. Tiempo ${formatDurationLong(frozenElapsed)}.", true)
        }
    }

    private fun updatePositionState() {
        if (!demoMode) return
        val point = route.pointAt(effectiveDistance()) ?: return
        latitude = point.first.lat
        longitude = point.first.lon
        headingDeg = point.second
    }

    private fun nextNote(): PaceNote? =
        notes.getOrNull(nextIndex)?.takeIf { it.type != NoteType.FINISH }

    private fun effectiveDistance(): Double = (rawDistanceM + offsetM).coerceAtLeast(0.0)

    private fun firstNoteAtOrAfter(distance: Double): Int {
        val idx = notes.indexOfFirst { it.distanceM >= distance && it.type != NoteType.START }
        return if (idx < 0) notes.size else idx
    }

    private fun resetProgress() {
        nextIndex = notes.indexOfFirst { it.type != NoteType.START }.let { if (it < 0) 0 else it }
        rawDistanceM = 0.0
        offsetM = 0.0
        smoothedSpeedMps = 0.0
        maxSpeedKmh = 0f
        headingDeg = 0.0
        frozenElapsed = 0L
        pauseAccum = 0L
        pausedAt = 0L
        finished = false
        lastFix = null
        lastFixAt = 0L
        demoDistance = 0.0
        demoSpeed = 0.0
        demoPoint = null
        trail.clear()
        addTrail(route.points.firstOrNull() ?: GeoPoint(40.8431, -3.9487))
        resetCalloutFlags()
    }

    private fun resetCalloutFlags() {
        calledIndex = -1
        reminderIndex = -1
        beepedIndex = -1
    }

    private fun addTrail(point: GeoPoint) {
        if (trail.size >= 900) trail.removeFirst()
        trail.addLast(point)
    }

    private fun elapsedMs(now: Long = SystemClock.elapsedRealtime()): Long =
        if (running && !paused) {
            (now - startedAt - pauseAccum).coerceAtLeast(0L)
        } else {
            frozenElapsed
        }

    private fun emit(now: Long = SystemClock.elapsedRealtime()) {
        val current = nextNote()
        val effective = effectiveDistance()
        val elapsed = elapsedMs(now)
        val kmh = (smoothedSpeedMps * 3.6).toFloat()
        val avg = if (elapsed > 3000 && rawDistanceM > 5) {
            (rawDistanceM / 1000.0) / (elapsed / 3_600_000.0).coerceAtLeast(1.0)
        } else 0.0
        val total = roadbook?.distanceM ?: 0.0
        _state.value = RallyState(
            roadbook = roadbook,
            running = running,
            paused = paused,
            finished = finished,
            speedKmh = if (paused) 0f else kmh,
            headingDeg = headingDeg.toFloat(),
            gpsFix = demoMode || (lastFixAt > 0 && now - lastFixAt < 5_000),
            accuracyM = lastFix?.accuracyM ?: 0f,
            latitude = latitude,
            longitude = longitude,
            stageDistanceM = effective,
            elapsedMs = elapsed,
            avgSpeedKmh = avg.toFloat(),
            maxSpeedKmh = maxSpeedKmh,
            nextIndex = nextIndex,
            distanceToNextM = current?.let { (it.distanceM - effective).coerceAtLeast(0.0) },
            nextNote = current,
            upcoming = notes.drop(nextIndex).filter { it.type != NoteType.FINISH }.take(3),
            trail = trail.toList(),
            demo = demoMode,
            voiceEnabled = settings().voiceEnabled,
            progress = if (total > 0) (effective / total).toFloat().coerceIn(0f, 1f) else 0f
        )
    }

    private fun Double.roundInt(): Int = this.roundToInt()
}
