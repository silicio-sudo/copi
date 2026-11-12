package com.silicio.copi

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silicio.copi.data.AppSettings
import com.silicio.copi.data.DemoRoute
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.CopiTheme
import com.silicio.copi.ui.screens.PlannerScreen
import com.silicio.copi.ui.screens.RoadbooksScreen
import com.silicio.copi.ui.screens.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = CopiApp::class, qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    private fun capture(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent { CopiTheme { content() } }
        ShadowLooper.idleMainLooper()

        val width = 822
        val height = 1782
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        decor.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        val output = File("build/reports/screenshots/$name-${BuildConfig.FLAVOR}.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun `captura el dashboard en marcha`() {
        val demo = DemoRoute.roadbook()
        val notes = demo.sortedNotes
        val state = RallyState(
            roadbook = demo,
            running = true,
            speedKmh = 68f,
            headingDeg = 42f,
            gpsFix = true,
            accuracyM = 4f,
            latitude = demo.route.getOrNull(20)?.lat,
            longitude = demo.route.getOrNull(20)?.lon,
            stageDistanceM = 420.0,
            elapsedMs = 192_000L,
            avgSpeedKmh = 61.4f,
            maxSpeedKmh = 88f,
            nextIndex = 2,
            distanceToNextM = 125.0,
            nextNote = notes.getOrNull(2),
            upcoming = notes.drop(2).take(3),
            trail = demo.route.take(30),
            demo = false,
            voiceEnabled = true,
            progress = 0.21f
        )
        capture("dashboard-hero") {
            com.silicio.copi.ui.screens.DashboardScreen(
                state = state,
                settings = AppSettings(),
                hasLocation = true,
                onRequestPermission = {},
                onOpenRoadbooks = {},
                onOpenPlanner = {}
            )
        }
    }

    @Test
    fun `captura la biblioteca de tramos`() {
        val demo = DemoRoute.roadbook()
        capture("tramos") {
            RoadbooksScreen(
                roadbooks = listOf(demo),
                settings = AppSettings(),
                state = RallyState(roadbook = demo),
                importing = false,
                onNavigateHome = {}
            )
        }
    }

    @Test
    fun `captura el detalle del tramo`() {
        val demo = DemoRoute.roadbook()
        capture("tramo") {
            RoadbooksScreen(
                roadbooks = listOf(demo),
                settings = AppSettings(),
                state = RallyState(
                    roadbook = demo,
                    latitude = demo.route.firstOrNull()?.lat,
                    longitude = demo.route.firstOrNull()?.lon,
                    nextIndex = 1
                ),
                importing = false,
                onNavigateHome = {},
                initialRoadbookId = demo.id
            )
        }
    }

    @Test
    fun `captura ajustes`() {
        capture("ajustes") {
            SettingsScreen(settings = AppSettings(), onOpenHelp = {}, onOpenAbout = {})
        }
    }

    @Test
    fun `captura planificador`() {
        capture("ruta") {
            PlannerScreen(settings = AppSettings(), state = RallyState(), onGenerated = {})
        }
    }
}
