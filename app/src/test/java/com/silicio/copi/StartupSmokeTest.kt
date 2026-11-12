package com.silicio.copi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silicio.copi.nav.Copi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = CopiApp::class)
class StartupSmokeTest {
    @Test
    fun `la aplicación inicializa el controlador sin errores`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
        assertNotNull(Copi.engine)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertNotNull(Copi.settings.current)
        assertTrue(Copi.engine.state.value.elapsedMs >= 0L)
    }
}
