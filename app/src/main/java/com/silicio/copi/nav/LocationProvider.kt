package com.silicio.copi.nav

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

data class LocationFix(
    val lat: Double,
    val lon: Double,
    val speedMps: Float,
    val bearingDeg: Float,
    val accuracyM: Float,
    val timeMs: Long
)

class LocationProvider(
    context: Context,
    private val onFix: (LocationFix) -> Unit
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onFix(location.toFix())
        }

        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission(appContext)) return
        val lm = manager ?: return
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return

        bestLastKnown()?.let(onFix)
        providers.forEach { provider ->
            runCatching {
                lm.requestLocationUpdates(
                    provider,
                    1_000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!hasPermission(appContext)) return
        runCatching { manager?.removeUpdates(listener) }
    }

    @SuppressLint("MissingPermission")
    fun bestLastKnown(): LocationFix? {
        if (!hasPermission(appContext)) return null
        val lm = manager ?: return null
        val locations = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
        val best = locations.maxByOrNull { it.time } ?: return null
        if (System.currentTimeMillis() - best.time > 10 * 60 * 1000) return null
        return best.toFix()
    }

    private fun Location.toFix() = LocationFix(
        lat = latitude,
        lon = longitude,
        speedMps = if (hasSpeed()) speed else -1f,
        bearingDeg = if (hasBearing()) bearing else -1f,
        accuracyM = if (hasAccuracy()) accuracy else 99f,
        timeMs = time
    )

    companion object {
        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
