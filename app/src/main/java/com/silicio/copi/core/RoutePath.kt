package com.silicio.copi.core

import com.silicio.copi.data.GeoPoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Geometría de una ruta con distancias acumuladas. Permite interpolar la
 * posición a una distancia dada y "encajar" el GPS sobre el trazado.
 */
class RoutePath(val points: List<GeoPoint>) {
    val cumulative: DoubleArray
    val totalM: Double

    init {
        cumulative = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumulative[i] = cumulative[i - 1] + distanceM(points[i - 1], points[i])
        }
        totalM = if (points.isEmpty()) 0.0 else cumulative.last()
    }

    val isUsable: Boolean get() = points.size >= 2 && totalM > 1.0

    fun pointAt(distanceM: Double): Pair<GeoPoint, Double>? {
        if (!isUsable) return null
        val d = distanceM.coerceIn(0.0, totalM)
        var lo = 0
        var hi = points.lastIndex
        while (lo < hi - 1) {
            val mid = (lo + hi) / 2
            if (cumulative[mid] <= d) lo = mid else hi = mid
        }
        val segStart = cumulative[lo]
        val segEnd = cumulative[lo + 1]
        val t = if (segEnd > segStart) ((d - segStart) / (segEnd - segStart)).coerceIn(0.0, 1.0) else 0.0
        val p = interpolatePoint(points[lo], points[lo + 1], t)
        val bearing = bearingDeg(points[lo], points[lo + 1])
        return p to bearing
    }

    /** Devuelve (distancia sobre la ruta, desvío perpendicular aproximado). */
    fun nearest(lat: Double, lon: Double): Pair<Double, Double>? {
        if (!isUsable) return null
        val target = GeoPoint(lat, lon)
        var bestDist = Double.MAX_VALUE
        var bestAlong = 0.0
        var bestOffset = Double.MAX_VALUE
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            val segLen = cumulative[i + 1] - cumulative[i]
            if (segLen < 0.01) continue
            val scale = Math.cos(Math.toRadians(lat))
            val ax = a.lon * scale
            val ay = a.lat
            val bx = b.lon * scale
            val by = b.lat
            val px = lon * scale
            val py = lat
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0) (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0) else 0.0
            val qx = ax + dx * t
            val qy = ay + dy * t
            val off = hypot(px - qx, py - qy) * 111_320.0
            val along = cumulative[i] + segLen * t
            if (off < bestOffset && off < bestDist) {
                bestDist = off
                bestAlong = along
                bestOffset = off
            }
        }
        return if (bestOffset == Double.MAX_VALUE) null else bestAlong to bestOffset
    }

    fun slice(fromM: Double, toM: Double): List<GeoPoint> {
        if (!isUsable) return points
        val start = pointAt(fromM)?.first ?: points.first()
        val end = pointAt(toM)?.first ?: points.last()
        val result = mutableListOf(start)
        for (i in points.indices) {
            if (cumulative[i] > fromM && cumulative[i] < toM) result.add(points[i])
        }
        result.add(end)
        return result
    }

    fun bounds(): Bounds? {
        if (points.isEmpty()) return null
        var minLat = points.first().lat
        var maxLat = minLat
        var minLon = points.first().lon
        var maxLon = minLon
        points.forEach {
            minLat = min(minLat, it.lat)
            maxLat = maxOf(maxLat, it.lat)
            minLon = min(minLon, it.lon)
            maxLon = maxOf(maxLon, it.lon)
        }
        return Bounds(minLat, maxLat, minLon, maxLon)
    }

    data class Bounds(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
        val latSpan: Double get() = (maxLat - minLat).coerceAtLeast(1e-6)
        val lonSpan: Double get() = (maxLon - minLon).coerceAtLeast(1e-6)
    }

    companion object {
        fun distanceBetweenOffsets(a: GeoPoint, b: GeoPoint): Double = abs(distanceM(a, b))
    }
}
