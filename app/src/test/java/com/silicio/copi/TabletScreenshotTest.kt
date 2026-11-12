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
import com.silicio.copi.ui.screens.DashboardScreen
import com.silicio.copi.ui.screens.RoadbooksScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

private fun captureTablet(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
    val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    activity.setContent { CopiTheme { content() } }
    ShadowLooper.idleMainLooper()

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

private fun tabletDashboardState(): RallyState {
    val demo = DemoRoute.roadbook()
    val notes = demo.sortedNotes
    return RallyState(
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
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = CopiApp::class, qualifiers = "w600dp-h960dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tablet7ScreenshotTest {
    @Test
    fun `captura dashboard 7 pulgadas`() {
        captureTablet("tablet7-dashboard", 1200, 1920) {
            DashboardScreen(
                state = tabletDashboardState(),
                settings = AppSettings(),
                hasLocation = true,
                onRequestPermission = {},
                onOpenRoadbooks = {},
                onOpenPlanner = {}
            )
        }
    }

    @Test
    fun `captura tramo 7 pulgadas`() {
        val demo = DemoRoute.roadbook()
        captureTablet("tablet7-tramo", 1200, 1920) {
            RoadbooksScreen(
                roadbooks = listOf(demo),
                settings = AppSettings(),
                state = RallyState(roadbook = demo, nextIndex = 2),
                importing = false,
                onNavigateHome = {},
                initialRoadbookId = demo.id
            )
        }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = CopiApp::class, qualifiers = "w800dp-h1280dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tablet10ScreenshotTest {
    @Test
    fun `captura dashboard 10 pulgadas`() {
        captureTablet("tablet10-dashboard", 1600, 2560) {
            DashboardScreen(
                state = tabletDashboardState(),
                settings = AppSettings(),
                hasLocation = true,
                onRequestPermission = {},
                onOpenRoadbooks = {},
                onOpenPlanner = {}
            )
        }
    }

    @Test
    fun `captura tramo 10 pulgadas`() {
        val demo = DemoRoute.roadbook()
        captureTablet("tablet10-tramo", 1600, 2560) {
            RoadbooksScreen(
                roadbooks = listOf(demo),
                settings = AppSettings(),
                state = RallyState(roadbook = demo, nextIndex = 2),
                importing = false,
                onNavigateHome = {},
                initialRoadbookId = demo.id
            )
        }
    }
}
