package com.silicio.copi

import com.silicio.copi.data.GeoPoint
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.Roadbook
import com.silicio.copi.data.RoadbookCodec
import com.silicio.copi.data.announcement
import com.silicio.copi.data.phraseEs
import com.silicio.copi.data.shortEs
import com.silicio.copi.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadbookCodecTest {
    @Test
    fun `parsea csv absoluto`() {
        val csv = """
            distancia_m,tipo,severidad,modificador,nota
            100,izquierda,4,larga,
            300,derecha,2,,
            600,meta,,,
        """.trimIndent()
        val roadbook = RoadbookCodec.parseCsv(csv, "CSV")
        assertEquals(4, roadbook.sortedNotes.size)
        assertEquals(NoteType.LEFT, roadbook.notes[1].type)
        assertEquals(4, roadbook.notes[1].severity)
        assertEquals("larga", roadbook.notes[1].modifier)
        assertEquals(NoteType.FINISH, roadbook.notes.last().type)
        assertEquals(600.0, roadbook.distanceM, 0.1)
    }

    @Test
    fun `parsea csv relativo y exporta`() {
        val csv = """
            0,salida,,,
            +120,horquilla izquierda,1,,
            +80,derecha,5,corta,
        """.trimIndent()
        val roadbook = RoadbookCodec.parseCsv(csv, "Relativo")
        assertEquals(NoteType.HAIRPIN_LEFT, roadbook.notes[1].type)
        assertEquals(120.0, roadbook.notes[1].distanceM, 0.1)
        assertEquals(200.0, roadbook.notes[2].distanceM, 0.1)
        val exported = RoadbookCodec.toCsv(roadbook)
        assertTrue(exported.contains("horquilla_left") || exported.contains("hairpin_left"))
        assertTrue(exported.contains("meta"))
    }

    @Test
    fun `json roundtrip`() {
        val roadbook = Roadbook(
            id = "id",
            name = "Tramo",
            createdAt = 1L,
            distanceM = 1000.0,
            notes = listOf(PaceNote(0.0, NoteType.START), PaceNote(1000.0, NoteType.FINISH)),
            route = listOf(GeoPoint(40.0, -3.0), GeoPoint(40.01, -3.0))
        )
        val decoded = RoadbookCodec.decode(RoadbookCodec.encode(roadbook))
        assertEquals(roadbook, decoded)
    }

    @Test
    fun `frases de voz`() {
        val note = PaceNote(100.0, NoteType.LEFT, severity = 4, modifier = "larga")
        assertEquals("izquierda 4, larga", note.phraseEs())
        assertEquals("IZQ 4", note.shortEs())
        assertTrue(note.announcement(120.0, UnitSystem.METRIC).startsWith("En 120 metros"))
        assertTrue(note.announcement(10.0, UnitSystem.METRIC).startsWith("Ahora"))
    }
}
