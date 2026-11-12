package com.silicio.copi.car

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.silicio.copi.R
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.formatDuration
import com.silicio.copi.core.formatSpeed
import com.silicio.copi.core.speedUnit
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.phraseEs
import com.silicio.copi.data.shortEs
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.LocationProvider
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.Brand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import kotlin.math.roundToInt

/**
 * Pantalla de Android Auto: mapa (superficie propia) + HUD de tramo + lista de
 * próximas notas. También informa a NavigationManager para clusters/HUD del
 * coche y para que Android Auto reconozca la navegación activa.
 */
@ExperimentalCarApi
class CopiCarScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private val brand = Brand.from(carContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val density = carContext.resources.displayMetrics.density
    private val scaledDensity = carContext.resources.displayMetrics.scaledDensity
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var visibleArea: Rect? = null

    private val navigationManager: NavigationManager by lazy {
        carContext.getCarService(NavigationManager::class.java)
    }
    private var navigationStarted = false
    private var lastTripUpdate = 0L
    private var lastTemplateKey: String? = null

    private val drawRunnable = object : Runnable {
        override fun run() {
            drawHud()
            handler.postDelayed(this, 250L)
        }
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        val executor = Executor { command -> handler.post(command) }
        navigationManager.setNavigationManagerCallback(
            executor,
            object : NavigationManagerCallback {
                override fun onStopNavigation() {
                    Copi.resetNavigation()
                    invalidate()
                }

                override fun onAutoDriveEnabled() {
                    Copi.setDemoMode(true)
                    Copi.startNavigation()
                    invalidate()
                }
            }
        )

        scope.launch {
            Copi.engine.state.collect { state ->
                updateNavigationManager(state)
                drawHud()
                val key = templateKey(state)
                if (key != lastTemplateKey) {
                    lastTemplateKey = key
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        invalidate()
                    }
                }
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            }

            override fun onStop(owner: LifecycleOwner) {
                handler.removeCallbacks(drawRunnable)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                handler.removeCallbacksAndMessages(null)
                scope.cancel()
                surface = null
                if (navigationStarted) {
                    runCatching { navigationManager.navigationEnded() }
                    navigationStarted = false
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val state = Copi.engine.state.value
        val roadbook = state.roadbook
        if (roadbook == null) {
            return MessageTemplate.Builder(
                "Abre Copi en el móvil para elegir o importar un tramo. " +
                    "Después podrás iniciarlo desde la pantalla del coche."
            )
                .setTitle("Copi · sin tramo")
                .build()
        }

        val settings = Copi.settings.current
        val items = ItemList.Builder().setNoItemsMessage("Sin notas pendientes")
        val upcoming = state.upcoming
        if (upcoming.isEmpty()) {
            items.addItem(
                Row.Builder()
                    .setTitle("Tramo completado")
                    .addText("Pulsa Reiniciar en el móvil o vuelve a iniciar.")
                    .build()
            )
        } else {
            upcoming.forEachIndexed { index, note ->
                val distance = (note.distanceM - state.stageDistanceM).coerceAtLeast(0.0)
                val row = Row.Builder()
                    .setTitle("${formatDistanceShort(distance, settings.units)} · ${note.shortEs()}")
                    .addText(
                        listOf(
                            if (index == 0) "AHORA" else "",
                            note.modifier,
                            note.phraseEs()
                        ).filter { it.isNotBlank() }.joinToString(" · ")
                    )
                    .setNumericDecoration(index + 1)
                    .build()
                items.addItem(row)
            }
        }

        val itemList = items.build()
        val content = ListTemplate.Builder()
            .setTitle("Copi · ${roadbook.name}")
            .setSingleList(itemList)
            .build()

        val primaryAction = Action.Builder()
            .setTitle(
                when {
                    state.paused -> "Reanudar"
                    state.running -> "Pausa"
                    else -> "Iniciar"
                }
            )
            .setOnClickListener {
                when {
                    state.paused -> Copi.resumeNavigation()
                    state.running -> Copi.pauseNavigation()
                    else -> Copi.startNavigation()
                }
                invalidate()
            }
            .build()

        val previousAction = Action.Builder()
            .setTitle("Anterior")
            .setOnClickListener {
                Copi.previousNote()
                invalidate()
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("Siguiente")
            .setOnClickListener {
                Copi.skipNext()
                invalidate()
            }
            .build()

        val muteAction = Action.Builder()
            .setTitle(if (settings.voiceEnabled) "Silenciar" else "Voz")
            .setOnClickListener {
                Copi.toggleMute()
                invalidate()
            }
            .build()

        val actions = ActionStrip.Builder()
            .addAction(primaryAction)
            .addAction(previousAction)
            .addAction(nextAction)
            .addAction(muteAction)
            .build()

        // MapWithContentTemplate necesita Car API 7. En hosts más antiguos
        // mostramos una lista con los controles esenciales para que la app
        // siga apareciendo en Android Auto.
        if (carContext.carAppApiLevel >= 7) {
            return MapWithContentTemplate.Builder()
                .setContentTemplate(content)
                .setActionStrip(actions)
                .setLoading(false)
                .build()
        }

        return ListTemplate.Builder()
            .setTitle("Copi · ${roadbook.name}")
            .setSingleList(itemList)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(primaryAction)
                    .addAction(nextAction)
                    .build()
            )
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        drawHud()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        if (surface === surfaceContainer.surface) {
            surface = null
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        // Se usa el área completa; el contenido de la plantilla se dibuja encima.
    }

    private fun drawHud() {
        val surface = surface ?: return
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        if (!surface.isValid) return
        val state = Copi.engine.state.value
        val settings = Copi.settings.current
        try {
            val canvas = surface.lockCanvas(null) ?: return
            try {
                render(canvas, state, settings)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (_: Throwable) {
            // La superficie puede desaparecer al cambiar de plantilla.
        }
    }

    private fun render(canvas: Canvas, state: RallyState, settings: com.silicio.copi.data.AppSettings) {
        val w = surfaceWidth.toFloat()
        val h = surfaceHeight.toFloat()
        canvas.drawColor(brand.bg.toArgb())

        // Franja rallye diagonal
        canvas.save()
        canvas.rotate(-18f, w * 0.85f, 0f)
        paint.color = brand.primary.copy(alpha = 0.16f).toArgb()
        canvas.drawRect(w * 0.62f, -h * 0.5f, w * 0.78f, h * 1.5f, paint)
        paint.color = brand.accent.copy(alpha = 0.10f).toArgb()
        canvas.drawRect(w * 0.79f, -h * 0.5f, w * 0.815f, h * 1.5f, paint)
        canvas.restore()

        val margin = 24f * density
        val top = 20f * density

        text(canvas, "COPI", margin, top + 30f * density, 34f, brand.text, italic = true, black = true)
        text(
            canvas,
            brand.edition,
            margin,
            top + 46f * density,
            9f,
            brand.primary,
            letterSpacing = 2.2f
        )
        if (brand.showPersonalization && brand.driver.isNotBlank()) {
            text(
                canvas,
                brand.driver,
                margin,
                top + 78f * density,
                17f,
                brand.text,
                italic = true,
                black = true,
                letterSpacing = 0.8f
            )
            text(
                canvas,
                "PILOTO",
                margin,
                top + 94f * density,
                8f,
                brand.muted,
                letterSpacing = 1.8f
            )
        }

        val status = when {
            state.demo -> "DEMO"
            state.gpsFix -> "GPS OK"
            else -> "SIN GPS"
        }
        val statusColor = when {
            state.demo -> brand.accent
            state.gpsFix -> brand.ok
            else -> brand.danger
        }
        paint.style = Paint.Style.FILL
        paint.color = statusColor.copy(alpha = 0.16f).toArgb()
        val pillWidth = 96f * density
        val pillHeight = 26f * density
        val pillLeft = w - margin - pillWidth
        canvas.drawRoundRect(
            pillLeft,
            top,
            pillLeft + pillWidth,
            top + pillHeight,
            pillHeight / 2,
            pillHeight / 2,
            paint
        )
        text(
            canvas,
            status,
            pillLeft + pillWidth / 2,
            top + 17f * density,
            9f,
            statusColor,
            align = Paint.Align.CENTER,
            letterSpacing = 1.4f
        )
        text(
            canvas,
            formatDuration(state.elapsedMs),
            w - margin,
            top + 52f * density,
            18f,
            brand.text,
            align = Paint.Align.RIGHT,
            italic = true,
            black = true
        )
        text(
            canvas,
            "${state.roadbook?.sortedNotes?.size ?: 0} NOTAS",
            w - margin,
            top + 68f * density,
            8f,
            brand.muted,
            align = Paint.Align.RIGHT,
            letterSpacing = 1.2f
        )

        val centerY = h * 0.36f
        val speedText = formatSpeed(state.speedKmh, settings.units)
        text(
            canvas,
            speedText,
            margin,
            centerY + 30f * density,
            82f,
            brand.text,
            italic = true,
            black = true
        )
        val speedWidth = paint.measureText(speedText)
        text(
            canvas,
            speedUnit(settings.units).uppercase(Locale("es", "ES")),
            margin + speedWidth + 8f * density,
            centerY + 8f * density,
            12f,
            brand.muted,
            letterSpacing = 1.6f
        )

        val note = state.nextNote
        val noteLeft = w * 0.44f
        val noteWidth = w - margin - noteLeft
        if (note != null) {
            val distance = state.distanceToNextM ?: 0.0
            text(
                canvas,
                formatDistanceShort(distance, settings.units).uppercase(Locale("es", "ES")),
                noteLeft,
                centerY - 52f * density,
                20f,
                brand.primary,
                italic = true,
                black = true
            )
            val label = note.shortEs()
            val labelSize = fitTextSize(label, noteWidth, 46f, 24f)
            text(
                canvas,
                label,
                noteLeft,
                centerY - 8f * density,
                labelSize,
                brand.text,
                italic = true,
                black = true
            )
            val subtitle = listOf(note.modifier, note.extra)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank {
                    if (note.instruction.isNotBlank()) note.instruction else ""
                }
            if (subtitle.isNotBlank()) {
                text(
                    canvas,
                    ellipsize(subtitle, noteWidth, 14f, brand.muted),
                    noteLeft,
                    centerY + 20f * density,
                    14f,
                    brand.muted
                )
            }
            text(
                canvas,
                "NOTA ${state.noteNumber}/${state.totalNotes}",
                noteLeft,
                centerY + 42f * density,
                9f,
                brand.muted,
                letterSpacing = 1.2f
            )
        } else if (state.finished) {
            text(
                canvas,
                "META",
                noteLeft,
                centerY - 8f * density,
                46f,
                brand.ok,
                italic = true,
                black = true
            )
            text(
                canvas,
                "Tramo completado",
                noteLeft,
                centerY + 20f * density,
                14f,
                brand.muted
            )
        } else {
            text(
                canvas,
                "SIN TRAMO",
                noteLeft,
                centerY - 8f * density,
                32f,
                brand.muted,
                italic = true,
                black = true
            )
        }

        // Barra de progreso
        val barY = h * 0.56f
        val barLeft = margin
        val barRight = w - margin
        paint.color = brand.line.toArgb()
        canvas.drawRoundRect(barLeft, barY, barRight, barY + 4f * density, 2f * density, 2f * density, paint)
        paint.color = brand.primary.toArgb()
        canvas.drawRoundRect(
            barLeft,
            barY,
            barLeft + (barRight - barLeft) * state.progress.coerceIn(0f, 1f),
            barY + 4f * density,
            2f * density,
            2f * density,
            paint
        )
        text(
            canvas,
            "${formatDistanceShort(state.stageDistanceM, settings.units)} / " +
                formatDistanceShort(state.roadbook?.distanceM ?: 0.0, settings.units),
            barLeft,
            barY + 20f * density,
            9f,
            brand.muted
        )
        text(
            canvas,
            "${formatSpeed(state.avgSpeedKmh, settings.units)} ${speedUnit(settings.units)} MEDIA",
            barRight,
            barY + 20f * density,
            9f,
            brand.muted,
            align = Paint.Align.RIGHT
        )

        if (!state.running && !state.paused) {
            val msg = when {
                !LocationProvider.hasPermission(carContext) && !settings.demoMode ->
                    "CONCEDE GPS EN EL MÓVIL O ACTIVA MODO DEMO"
                else -> "PULSA INICIAR PARA CANTAR EL TRAMO"
            }
            text(
                canvas,
                msg,
                w / 2,
                h * 0.66f,
                11f,
                brand.accent,
                align = Paint.Align.CENTER,
                letterSpacing = 1.2f
            )
        }
    }

    private fun text(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        sizeSp: Float,
        color: androidx.compose.ui.graphics.Color,
        align: Paint.Align = Paint.Align.LEFT,
        italic: Boolean = false,
        black: Boolean = false,
        letterSpacing: Float = 0f
    ) {
        paint.typeface = Typeface.create(
            if (black) "sans-serif-black" else "sans-serif",
            when {
                italic -> Typeface.BOLD_ITALIC
                black -> Typeface.BOLD
                else -> Typeface.NORMAL
            }
        )
        paint.textSize = sizeSp * scaledDensity
        paint.color = color.toArgb()
        paint.textAlign = align
        paint.letterSpacing = letterSpacing / 100f
        canvas.drawText(value, x, y, paint)
        paint.letterSpacing = 0f
    }

    private fun fitTextSize(text: String, maxWidth: Float, preferredSp: Float, minSp: Float): Float {
        var size = preferredSp
        while (size > minSp) {
            paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC)
            paint.textSize = size * scaledDensity
            if (paint.measureText(text) <= maxWidth) break
            size -= 2f
        }
        return size
    }

    private fun ellipsize(text: String, maxWidth: Float, sizeSp: Float, color: androidx.compose.ui.graphics.Color): String {
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = sizeSp * scaledDensity
        if (paint.measureText(text) <= maxWidth) return text
        var value = text
        while (value.length > 1 && paint.measureText("$value…") > maxWidth) {
            value = value.dropLast(2)
        }
        return "$value…"
    }

    private fun templateKey(state: RallyState): String = buildString {
        append(state.roadbook?.id)
        append('|').append(state.running)
        append('|').append(state.paused)
        append('|').append(state.finished)
        append('|').append(state.nextIndex)
        append('|').append(state.voiceEnabled)
        append('|').append(state.upcoming.size)
    }

    private fun updateNavigationManager(state: RallyState) {
        val shouldNavigate = state.running || state.paused
        if (shouldNavigate && !navigationStarted) {
            runCatching { navigationManager.navigationStarted() }
            navigationStarted = true
        }
        if (!shouldNavigate && navigationStarted) {
            runCatching { navigationManager.navigationEnded() }
            navigationStarted = false
        }
        val now = System.currentTimeMillis()
        if (navigationStarted && now - lastTripUpdate > 1_500) {
            lastTripUpdate = now
            runCatching { navigationManager.updateTrip(buildTrip(state)) }
        }
    }

    private fun buildTrip(state: RallyState): Trip {
        val note = state.nextNote
            ?: PaceNote(0.0, NoteType.FINISH, instruction = "Meta")
        val remaining = (state.distanceToNextM ?: 0.0).coerceAtLeast(0.0)
        val units = Copi.settings.current.units
        val distance = if (units == com.silicio.copi.data.UnitSystem.IMPERIAL) {
            Distance.create(remaining / 1609.344, Distance.UNIT_MILES)
        } else {
            Distance.create(remaining / 1000.0, Distance.UNIT_KILOMETERS)
        }
        val seconds = if (state.speedKmh > 3f) {
            (remaining / (state.speedKmh / 3.6)).toLong().coerceIn(5L, 86_400L)
        } else {
            60L
        }
        val arrival = DateTimeWithZone.create(
            System.currentTimeMillis() + seconds * 1000,
            TimeZone.getDefault()
        )
        val estimate = TravelEstimate.Builder(distance, arrival)
            .setRemainingTimeSeconds(seconds)
            .build()
        val step = Step.Builder(note.shortEs())
            .setManeuver(Maneuver.Builder(maneuverFor(note.type)).build())
            .setRoad(note.instruction.ifBlank { note.shortEs() })
            .build()
        val destination = Destination.Builder()
            .setName("Meta del tramo")
            .setAddress(state.roadbook?.name ?: "Copi")
            .build()
        return Trip.Builder()
            .addStep(step, estimate)
            .addDestination(destination, estimate)
            .setCurrentRoad(state.roadbook?.name ?: "Copi")
            .setLoading(false)
            .build()
    }

    private fun maneuverFor(type: NoteType): Int = when (type) {
        NoteType.LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
        NoteType.RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
        NoteType.HAIRPIN_LEFT -> Maneuver.TYPE_U_TURN_LEFT
        NoteType.HAIRPIN_RIGHT -> Maneuver.TYPE_U_TURN_RIGHT
        NoteType.KEEP_LEFT -> Maneuver.TYPE_KEEP_LEFT
        NoteType.KEEP_RIGHT -> Maneuver.TYPE_KEEP_RIGHT
        NoteType.STRAIGHT, NoteType.CREST, NoteType.JUMP -> Maneuver.TYPE_STRAIGHT
        NoteType.FINISH -> Maneuver.TYPE_DESTINATION
        else -> Maneuver.TYPE_UNKNOWN
    }
}
