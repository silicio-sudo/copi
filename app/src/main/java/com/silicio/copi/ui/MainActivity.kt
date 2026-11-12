package com.silicio.copi.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.silicio.copi.nav.Copi
import com.silicio.copi.nav.LocationProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasLocation by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocation = LocationProvider.hasPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        hasLocation = LocationProvider.hasPermission(this)

        if (!hasLocation) {
            requestPermissions()
        }

        lifecycleScope.launch {
            Copi.settings.state.collect { settings ->
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            CopiTheme {
                CopiRoot(
                    hasLocation = hasLocation,
                    onRequestPermission = ::requestPermissions
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasLocation = LocationProvider.hasPermission(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val query = when (data.scheme) {
            "geo" -> data.getQueryParameter("q")
                ?: data.schemeSpecificPart.takeIf { it.isNotBlank() && !it.startsWith("0,0") }
            "google.navigation" -> data.getQueryParameter("q")
            else -> null
        }
        if (!query.isNullOrBlank()) {
            Copi.setPendingDestination(query)
        }
    }
}
