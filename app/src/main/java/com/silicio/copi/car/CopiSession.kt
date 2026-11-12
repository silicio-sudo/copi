package com.silicio.copi.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.annotations.ExperimentalCarApi
import com.silicio.copi.nav.Copi

@ExperimentalCarApi
class CopiSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        handle(intent)
        return CopiCarScreen(carContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val data = intent.data ?: return
        val query = when (data.scheme) {
            "geo" -> data.getQueryParameter("q")
                ?: data.schemeSpecificPart.takeIf { it.isNotBlank() && !it.startsWith("0,0") }
            "google.navigation" -> data.getQueryParameter("q")
            else -> null
        }
        if (!query.isNullOrBlank()) {
            Copi.handleNavigationIntent(query)
        }
    }
}
