package com.silicio.copi.data

import com.silicio.copi.core.bearingDeg
import com.silicio.copi.core.distanceM
import com.silicio.copi.core.interpolatePoint
import com.silicio.copi.core.shortestAngleDelta
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Genera pacenotes a partir de la geometría de una ruta (GPX, OSRM o demo):
 * detecta curvas, calcula el ángulo recorrido y las clasifica de 1 (horquilla)
 * a 6 (curva rápida). También detecta crestas si el GPX trae altitud.
 */
object PacenoteGenerator {
    private const val STEP_M = 8.0
    private const val MIN_CORNER_ANGLE = 22.0
    private const val WINDOW = 2

    private data class Sample(val point: GeoPoint, val distM: Double)
    private data class Corner(
        val entryDist: Double,
        val exitDist: Double,
        val peakDist: Double,
        val angle: Double,
        val lengthM: Double
    )

    fun fromRoute(
        points: List<GeoPoint>,
        name: String,
        source: String,
        id: String = UUID.randomUUID().toString(),
        createdAt: Long = System.currentTimeMillis()
    ): Roadbook {
        val clean = mutableListOf<GeoPoint>()
        points.forEach { p ->
            if (p.lat.isFinite() && p.lon.isFinite() &&
                p.lat in -90.0..90.0 && p.lon in -180.0..180.0
            ) {
                if (clean.isEmpty() || distanceM(clean.last(), p) > 1.0) clean.add(p)
            }
        }
        if (clean.size < 2) {
            return basicRoadbook(name, source, id, createdAt, 0.0)
        }

        val samples = resample(clean, STEP_M)
        val total = samples.last().distM
        if (samples.size < 5 || total < 30.0) {
            return basicRoadbook(name, source, id, createdAt, total, clean)
        }

        val headings = smoothedHeadings(samples)
        val rawDelta = DoubleArray(samples.size)
        for (i in samples.indices) {
            val before = headings[(i - WINDOW).coerceAtLeast(0)]
            val after = headings[(i + WINDOW).coerceAtMost(samples.size - 1)]
            rawDelta[i] = shortestAngleDelta(before, after)
        }
        val delta = movingAverage(rawDelta, 2)

        val runs = detectPeaks(delta)
        val corners = runs.mapNotNull { peak ->
            var start = peak
            var guard = 0
            while (start > 1 &&
                abs(shortestAngleDelta(headings[start - 1], headings[start])) > 1.5 &&
                guard++ < 6
            ) {
                start--
            }
            var end = peak
            guard = 0
            while (end < samples.lastIndex - 1 &&
                abs(shortestAngleDelta(headings[end], headings[end + 1])) > 1.5 &&
                guard++ < 6
            ) {
                end++
            }
            start = (start - 1).coerceAtLeast(0)
            end = (end + 1).coerceAtMost(samples.lastIndex)
            val angle = shortestAngleDelta(headings[start], headings[end])
            if (abs(angle) < MIN_CORNER_ANGLE) return@mapNotNull null
            val entryDist = samples[start].distM
            val exitDist = samples[end].distM
            if (entryDist < 12.0 || entryDist > total - 12.0) return@mapNotNull null
            Corner(
                entryDist = entryDist,
                exitDist = exitDist,
                peakDist = samples[peak].distM,
                angle = angle,
                lengthM = max(12.0, exitDist - entryDist)
            )
        }.sortedBy { it.entryDist }

        val merged = corners.filterIndexed { index, corner ->
            index == 0 || abs(corner.peakDist - corners[index - 1].peakDist) > 20.0
        }
        val cornerNotes = merged.map { corner ->
            val severity = severityFor(abs(corner.angle))
            val type = when {
                severity == 1 && corner.angle > 0 -> NoteType.HAIRPIN_RIGHT
                severity == 1 -> NoteType.HAIRPIN_LEFT
                corner.angle > 0 -> NoteType.RIGHT
                else -> NoteType.LEFT
            }
            val modifier = modifierFor(corner)
            PaceNote(
                distanceM = corner.entryDist.coerceAtLeast(0.0),
                type = type,
                severity = severity,
                modifier = modifier
            )
        }

        val crestNotes = detectCrests(samples, cornerNotes)
        val notes = buildList {
            add(PaceNote(0.0, NoteType.START, instruction = "Salida"))
            addAll(cornerNotes)
            addAll(crestNotes)
            add(PaceNote(total, NoteType.FINISH))
        }.sortedBy { it.distanceM }

        return Roadbook(
            id = id,
            name = name,
            createdAt = createdAt,
            distanceM = total,
            notes = dedupeNear(notes),
            route = clean,
            source = source
        )
    }

    private fun basicRoadbook(
        name: String,
        source: String,
        id: String,
        createdAt: Long,
        total: Double,
        route: List<GeoPoint> = emptyList()
    ) = Roadbook(
        id = id,
        name = name,
        createdAt = createdAt,
        distanceM = total,
        notes = listOf(
            PaceNote(0.0, NoteType.START, instruction = "Salida"),
            PaceNote(total, NoteType.FINISH)
        ),
        route = route,
        source = source
    )

    private fun resample(points: List<GeoPoint>, step: Double): List<Sample> {
        val cumulative = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumulative[i] = cumulative[i - 1] + distanceM(points[i - 1], points[i])
        }
        val total = cumulative.last()
        val result = mutableListOf<Sample>()
        var segment = 0
        var target = 0.0
        while (target < total) {
            while (segment < points.lastIndex - 1 && cumulative[segment + 1] < target) segment++
            val segStart = cumulative[segment]
            val segEnd = cumulative[segment + 1]
            val t = if (segEnd > segStart) (target - segStart) / (segEnd - segStart) else 0.0
            result.add(
                Sample(
                    point = interpolatePoint(points[segment], points[segment + 1], t),
                    distM = target
                )
            )
            target += step
        }
        if (result.isEmpty() || result.last().distM < total) {
            result.add(Sample(points.last(), total))
        }
        return result
    }

    private fun smoothedHeadings(samples: List<Sample>): DoubleArray {
        val raw = DoubleArray(samples.size)
        for (i in samples.indices) {
            val a = samples[i]
            val b = samples[(i + 1).coerceAtMost(samples.lastIndex)]
            raw[i] = if (i == samples.lastIndex) raw[(i - 1).coerceAtLeast(0)]
            else bearingDeg(a.point, b.point)
        }
        val out = DoubleArray(raw.size)
        for (i in raw.indices) {
            var sx = 0.0
            var sy = 0.0
            for (j in (i - 2)..(i + 2)) {
                val idx = j.coerceIn(0, raw.lastIndex)
                val rad = Math.toRadians(raw[idx])
                sx += Math.cos(rad)
                sy += Math.sin(rad)
            }
            out[i] = (Math.toDegrees(kotlin.math.atan2(sy, sx)) + 360.0) % 360.0
        }
        return out
    }

    private fun movingAverage(values: DoubleArray, radius: Int): DoubleArray {
        val out = DoubleArray(values.size)
        for (i in values.indices) {
            var sum = 0.0
            var count = 0
            for (j in (i - radius)..(i + radius)) {
                sum += values[j.coerceIn(0, values.lastIndex)]
                count++
            }
            out[i] = sum / count
        }
        return out
    }

    private fun detectPeaks(delta: DoubleArray): List<Int> {
        val peaks = mutableListOf<Int>()
        for (i in 2 until delta.lastIndex - 1) {
            val magnitude = abs(delta[i])
            if (magnitude < 10.0) continue
            val isPeak = magnitude >= abs(delta[i - 1]) &&
                magnitude >= abs(delta[i + 1]) &&
                (magnitude > abs(delta[i - 1]) || magnitude > abs(delta[i + 1]))
            if (!isPeak) continue
            val last = peaks.lastOrNull()
            if (last != null && i - last <= 4) {
                if (magnitude > abs(delta[last])) peaks[peaks.lastIndex] = i
            } else {
                peaks.add(i)
            }
        }
        return peaks
    }
    private fun mergeCorners(corners: List<Corner>): List<Corner> {
        if (corners.isEmpty()) return emptyList()
        val out = mutableListOf<Corner>()
        var current = corners.first()
        for (next in corners.drop(1)) {
            val gap = next.entryDist - current.exitDist
            val sameDirection = (current.angle > 0 && next.angle > 0) || (current.angle < 0 && next.angle < 0)
            if (gap in -5.0..30.0 && sameDirection) {
                val combined = if (abs(next.angle) > abs(current.angle)) next.angle else current.angle
                current = Corner(
                    entryDist = current.entryDist,
                    exitDist = max(current.exitDist, next.exitDist),
                    peakDist = if (abs(next.angle) > abs(current.angle)) next.peakDist else current.peakDist,
                    angle = combined,
                    lengthM = max(current.lengthM, next.lengthM)
                )
            } else {
                out.add(current)
                current = next
            }
        }
        out.add(current)
        return out
    }

    private fun severityFor(absAngle: Double): Int = when {
        absAngle >= 160 -> 1
        absAngle >= 135 -> 2
        absAngle >= 105 -> 3
        absAngle >= 75 -> 4
        absAngle >= 45 -> 5
        else -> 6
    }

    private fun modifierFor(corner: Corner): String {
        val severity = severityFor(abs(corner.angle))
        if (severity <= 2) return ""
        return when {
            corner.lengthM >= 70 -> "larga"
            corner.lengthM <= 24 -> "corta"
            else -> ""
        }
    }

    private fun detectCrests(samples: List<Sample>, corners: List<PaceNote>): List<PaceNote> {
        if (samples.size < 12 || samples.any { it.point.alt == null }) return emptyList()
        val notes = mutableListOf<PaceNote>()
        for (i in 6 until samples.size - 6) {
            val before = samples[i - 5]
            val after = samples[i + 5]
            val a = before.point.alt ?: continue
            val b = after.point.alt ?: continue
            val run = after.distM - before.distM
            if (run < 20) continue
            val slope = (b - a) / run
            val peak = samples[i].point.alt ?: continue
            val prev = samples[i - 4].point.alt ?: continue
            val next = samples[i + 4].point.alt ?: continue
            val beforeSlope = (peak - prev) / (samples[i].distM - samples[i - 4].distM).coerceAtLeast(1.0)
            val afterSlope = (next - peak) / (samples[i + 4].distM - samples[i].distM).coerceAtLeast(1.0)
            val isCrest = peak - prev > 2.0 && peak - next > 2.0 &&
                beforeSlope > 0.004 && afterSlope < -0.004 && abs(slope) < 0.05
            if (isCrest) {
                val dist = samples[i].distM
                if (dist < 25 || dist > samples.last().distM - 25) continue
                if (corners.any { abs(it.distanceM - dist) < 35 }) continue
                notes.add(PaceNote(dist, NoteType.CREST, instruction = "cresta"))
            }
        }
        return notes.distinctBy { (it.distanceM / 30).toInt() }
    }

    private fun dedupeNear(notes: List<PaceNote>): List<PaceNote> {
        val result = mutableListOf<PaceNote>()
        notes.sortedBy { it.distanceM }.forEach { note ->
            val prev = result.lastOrNull()
            if (prev == null || note.distanceM - prev.distanceM > 8.0 || note.type == NoteType.FINISH) {
                result.add(note)
            }
        }
        return result
    }
}
