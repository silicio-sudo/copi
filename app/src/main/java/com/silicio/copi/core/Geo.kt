package com.silicio.copi.core

import com.silicio.copi.data.GeoPoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val EARTH_RADIUS_M = 6_371_008.8
private const val DEG_TO_RAD = Math.PI / 180.0
private const val RAD_TO_DEG = 180.0 / Math.PI

/** Distancia ortodrómica en metros. */
fun distanceM(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = a.lat * DEG_TO_RAD
    val lat2 = b.lat * DEG_TO_RAD
    val dLat = (b.lat - a.lat) * DEG_TO_RAD
    val dLon = (b.lon - a.lon) * DEG_TO_RAD
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
}

/** Rumbo inicial de a hacia b, en grados [0, 360). */
fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = a.lat * DEG_TO_RAD
    val lat2 = b.lat * DEG_TO_RAD
    val dLon = (b.lon - a.lon) * DEG_TO_RAD
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return normalizeBearing(atan2(y, x) * RAD_TO_DEG)
}

/** Punto destino a [distanceM] metros con rumbo [bearing]. */
fun offsetPoint(from: GeoPoint, bearing: Double, distanceM: Double): GeoPoint {
    val angular = distanceM / EARTH_RADIUS_M
    val brng = bearing * DEG_TO_RAD
    val lat1 = from.lat * DEG_TO_RAD
    val lon1 = from.lon * DEG_TO_RAD
    val lat2 = kotlin.math.asin(
        sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(brng)
    )
    val lon2 = lon1 + atan2(
        sin(brng) * sin(angular) * cos(lat1),
        cos(angular) - sin(lat1) * sin(lat2)
    )
    return GeoPoint(lat2 * RAD_TO_DEG, normalizeLon(lon2 * RAD_TO_DEG), from.alt)
}

fun interpolatePoint(a: GeoPoint, b: GeoPoint, t: Double): GeoPoint {
    val f = t.coerceIn(0.0, 1.0)
    val alt = if (a.alt != null && b.alt != null) a.alt + (b.alt - a.alt) * f else a.alt ?: b.alt
    return GeoPoint(
        lat = a.lat + (b.lat - a.lat) * f,
        lon = a.lon + (b.lon - a.lon) * f,
        alt = alt
    )
}

fun normalizeBearing(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

fun normalizeLon(lon: Double): Double {
    var l = lon
    while (l > 180.0) l -= 360.0
    while (l < -180.0) l += 360.0
    return l
}

/** Diferencia angular firmada más corta: positivo = giro a la derecha. */
fun shortestAngleDelta(from: Double, to: Double): Double {
    var d = normalizeBearing(to) - normalizeBearing(from)
    if (d > 180.0) d -= 360.0
    if (d < -180.0) d += 360.0
    return d
}

fun headingCardinal(deg: Double): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val idx = (((deg % 360) + 360) % 360 / 45.0).roundToInt() % 8
    return dirs[idx]
}

fun Double.roundTo(value: Int): Double {
    if (value <= 0) return this
    val factor = Math.pow(10.0, value.toDouble())
    return Math.round(this * factor) / factor
}

fun Float.toDoubleSafe(): Double = if (isFinite()) toDouble() else 0.0

fun formatCoord(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.5f, %.5f", lat, lon)

fun approxEqual(a: GeoPoint, b: GeoPoint, toleranceM: Double = 2.0): Boolean =
    distanceM(a, b) <= toleranceM && abs(a.lat - b.lat) < 1.0
