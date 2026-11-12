package com.silicio.copi.core

import com.silicio.copi.data.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

private val ES: Locale = Locale("es", "ES")

/** Velocidad en la unidad configurada (km/h o mph). */
fun speedValue(kmh: Float, units: UnitSystem): Float =
    if (units == UnitSystem.METRIC) kmh else (kmh * 0.621371f)

fun speedUnit(units: UnitSystem): String =
    if (units == UnitSystem.METRIC) "km/h" else "mph"

fun formatSpeed(kmh: Float, units: UnitSystem): String =
    speedValue(kmh, units).roundToInt().toString()

/** Distancia corta para UI: "120 m", "1,2 km", "420 ft", "0,8 mi". */
fun formatDistanceShort(meters: Double, units: UnitSystem, decimalsKm: Int = 1): String {
    if (units == UnitSystem.IMPERIAL) {
        val feet = meters * 3.28084
        return if (feet < 1000) {
            "${(feet / 10).roundToInt() * 10} ft"
        } else {
            String.format(ES, "%.${decimalsKm}f mi", meters / 1609.344)
        }
    }
    return if (meters < 1000) {
        "${(meters / 5).roundToInt() * 5} m"
    } else if (meters < 10_000) {
        String.format(ES, "%.${decimalsKm}f km", meters / 1000.0)
    } else {
        "${(meters / 1000.0).roundToInt()} km"
    }
}

/** Distancia hablada: "120 metros", "1,2 kilómetros". */
fun formatDistanceSpoken(meters: Double, units: UnitSystem): String {
    if (units == UnitSystem.IMPERIAL) {
        val feet = meters * 3.28084
        return if (feet < 1000) {
            "${(feet / 50).roundToInt() * 50} pies"
        } else {
            String.format(ES, "%.1f millas", meters / 1609.344)
        }
    }
    return when {
        meters < 1000 -> "${(meters / 10).roundToInt() * 10} metros"
        else -> String.format(ES, "%.1f kilómetros", meters / 1000.0)
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(ES, "%d:%02d:%02d", h, m, s)
    else String.format(ES, "%d:%02d", m, s)
}

fun formatDurationLong(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "$h h $m min $s s"
        m > 0 -> "$m min $s s"
        else -> "$s s"
    }
}

fun formatPace(secondsPerKm: Double): String {
    if (secondsPerKm <= 0 || !secondsPerKm.isFinite()) return "—"
    val mins = (secondsPerKm / 60).toInt()
    val secs = (secondsPerKm % 60).roundToInt()
    return String.format(ES, "%d:%02d /km", mins, secs)
}
