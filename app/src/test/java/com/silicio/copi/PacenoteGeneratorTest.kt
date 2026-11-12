package com.silicio.copi

import com.silicio.copi.data.GeoPoint
import com.silicio.copi.core.bearingDeg
import com.silicio.copi.core.distanceM
import com.silicio.copi.core.offsetPoint
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PacenoteGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PacenoteGeneratorTest {

    private fun cornerRoute(): List<GeoPoint> {
        val start = GeoPoint(0.0, 0.0)
        val p1 = offsetPoint(start, 0.0, 300.0)
        val p2 = offsetPoint(p1, 90.0, 600.0)
        return listOf(start, p1, p2)
    }

    @Test
    fun `genera salida meta y una curva a la derecha`() {
        val roadbook = PacenoteGenerator.fromRoute(cornerRoute(), "Test", "gpx")
        assertEquals(NoteType.START, roadbook.sortedNotes.first().type)
        assertEquals(NoteType.FINISH, roadbook.sortedNotes.last().type)
        assertTrue("distance=${roadbook.distanceM} notes=${roadbook.notes}", roadbook.distanceM > 800.0)
        val corners = roadbook.notes.filter { it.isCorner }
        assertEquals("notes=${roadbook.notes}", 1, corners.size)
        val corner = corners.first()
        assertEquals(NoteType.RIGHT, corner.type)
        assertTrue(corner.severity in 3..4)
        assertTrue(corner.distanceM in 250.0..350.0)
    }

    @Test
    fun `detecta horquilla`() {
        val start = GeoPoint(0.0, 0.0)
        val p1 = offsetPoint(start, 0.0, 250.0)
        val p2 = offsetPoint(p1, 168.0, 250.0)
        val roadbook = PacenoteGenerator.fromRoute(listOf(start, p1, p2), "Horquilla", "gpx")
        val corner = roadbook.notes.firstOrNull { it.isCorner }
        assertNotNull("notes=${roadbook.notes}", corner)
        assertTrue(
            corner!!.type == NoteType.HAIRPIN_RIGHT ||
                corner.type == NoteType.HAIRPIN_LEFT
        )
        assertEquals(1, corner.severity)
    }

    @Test
    fun `las notas estan ordenadas y sin duplicados cercanos`() {
        val roadbook = PacenoteGenerator.fromRoute(cornerRoute(), "Test", "gpx")
        val notes = roadbook.sortedNotes
        assertEquals(notes, roadbook.notes)
        notes.zipWithNext().forEach { (a, b) ->
            assertTrue(b.distanceM >= a.distanceM)
        }
        assertTrue(notes.size <= 8)
    }

    @Test
    fun `ruta vacia devuelve roadbook basico`() {
        val roadbook = PacenoteGenerator.fromRoute(emptyList(), "Vacío", "gpx")
        assertEquals(0.0, roadbook.distanceM, 0.001)
        assertEquals(2, roadbook.notes.size)
    }

    @Test
    fun `el tramo demo genera un roadbook util`() {
        val roadbook = com.silicio.copi.data.DemoRoute.roadbook()
        assertTrue("notas=${roadbook.notes.size}", roadbook.notes.size >= 8)
        assertTrue(roadbook.distanceM > 1500.0)
        val corners = roadbook.notes.filter { it.isCorner }
        assertTrue(corners.size >= 5)
        assertTrue(corners.any { it.severity == 1 })
        corners.zipWithNext().forEach { (a, b) ->
            assertTrue("notas demasiado juntas: $a / $b", b.distanceM - a.distanceM > 8.0)
        }
    }
}

class GeoTest {
    @Test
    fun `distancia y rumbo basicos`() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(0.0, 1.0)
        assertEquals(111_195.0, distanceM(a, b), 500.0)
        assertEquals(90.0, bearingDeg(a, b), 0.5)
    }

    @Test
    fun `offset point es coherente`() {
        val a = GeoPoint(40.0, -3.0)
        val b = offsetPoint(a, 45.0, 1000.0)
        assertEquals(1000.0, distanceM(a, b), 2.0)
        assertTrue(abs(bearingDeg(a, b) - 45.0) < 0.5)
    }

    @Test
    fun `ruta de esquina mide 900 metros`() {
        val p0 = GeoPoint(0.0, 0.0)
        val p1 = offsetPoint(p0, 0.0, 300.0)
        val p2 = offsetPoint(p1, 90.0, 300.0)
        val p3 = offsetPoint(p2, 0.0, 300.0)
        assertEquals(900.0, distanceM(p0, p1) + distanceM(p1, p2) + distanceM(p2, p3), 3.0)
        assertTrue(bearingDeg(p1, p2) in 80.0..100.0)
    }
}
