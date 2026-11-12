package com.silicio.copi.data

import kotlinx.serialization.json.Json
import java.util.Locale

object RoadbookCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(roadbook: Roadbook): String = json.encodeToString(Roadbook.serializer(), roadbook)

    fun decode(text: String): Roadbook = json.decodeFromString(Roadbook.serializer(), text)

    fun toCsv(roadbook: Roadbook): String {
        val sb = StringBuilder()
        sb.append("distancia_m,tipo,severidad,modificador,nota\n")
        roadbook.sortedNotes.forEach { note ->
            sb.append(String.format(Locale.US, "%.1f", note.distanceM)).append(',')
            sb.append(note.type.name.lowercase(Locale.ROOT)).append(',')
            sb.append(note.severity).append(',')
            sb.append(note.modifier.replace(",", " ")).append(',')
            sb.append((note.instruction.ifBlank { note.phraseEs() }).replace(",", " "))
            sb.append('\n')
        }
        return sb.toString()
    }

    fun parseCsv(text: String, fallbackName: String): Roadbook {
        val lines = text.lineSequence()
            .map { it.trim().removePrefix("\uFEFF") }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .toList()
        if (lines.isEmpty()) error("El CSV está vacío")

        val delimiter = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
        val rows = lines.map { line -> line.split(delimiter).map { it.trim().trim('"') } }
        val hasHeader = rows.first().firstOrNull()?.lowercase(Locale.ROOT)
            ?.let { it.contains("dist") || it.contains("tipo") } == true
        val data = if (hasHeader) rows.drop(1) else rows

        var relative = 0.0
        val parsed = data.mapNotNull { cols ->
            if (cols.size < 2) return@mapNotNull null
            val rawDistance = cols[0].replace("m", "", ignoreCase = true).replace(" ", "")
            val isRelative = rawDistance.startsWith("+")
            val distanceValue = rawDistance.removePrefix("+").replace(',', '.').toDoubleOrNull()
                ?: return@mapNotNull null
            val type = parseType(cols.getOrNull(1) ?: "")
            val severity = cols.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 6) ?: defaultSeverity(type)
            val modifier = cols.getOrNull(3).orEmpty()
            val instruction = cols.getOrNull(4).orEmpty()
            val distance = if (isRelative) {
                relative += distanceValue
                relative
            } else {
                distanceValue.also { relative = it }
            }
            PaceNote(
                distanceM = distance,
                type = type,
                severity = severity,
                modifier = modifier,
                instruction = instruction
            )
        }.sortedBy { it.distanceM }

        require(parsed.isNotEmpty()) { "No se encontraron notas válidas en el CSV" }
        val total = maxOf(parsed.last().distanceM, 0.0)
        val withStart = if (parsed.first().distanceM > 1.0) {
            listOf(PaceNote(0.0, NoteType.START, instruction = "Salida")) + parsed
        } else parsed
        val notes = if (withStart.last().type == NoteType.FINISH) withStart
        else withStart + PaceNote(total, NoteType.FINISH)
        return Roadbook(
            id = java.util.UUID.randomUUID().toString(),
            name = fallbackName,
            createdAt = System.currentTimeMillis(),
            distanceM = total,
            notes = notes,
            source = "csv"
        )
    }

    private fun parseType(raw: String): NoteType {
        val token = raw.lowercase(Locale.ROOT).trim()
        return when {
            token.startsWith("horquilla izq") || token == "hl" || token == "hairpin_l" -> NoteType.HAIRPIN_LEFT
            token.startsWith("horquilla der") || token == "hr" || token == "hairpin_r" -> NoteType.HAIRPIN_RIGHT
            token.startsWith("izq") || token == "l" || token == "left" -> NoteType.LEFT
            token.startsWith("der") || token == "r" || token == "right" -> NoteType.RIGHT
            token.startsWith("recto") || token == "s" || token == "straight" -> NoteType.STRAIGHT
            token.startsWith("mant") && token.contains("izq") -> NoteType.KEEP_LEFT
            token.startsWith("mant") && token.contains("der") -> NoteType.KEEP_RIGHT
            token.startsWith("crest") || token == "cresta" -> NoteType.CREST
            token.startsWith("salto") || token == "jump" -> NoteType.JUMP
            token.startsWith("peligro") || token.startsWith("caution") -> NoteType.CAUTION
            token.startsWith("chicane") -> NoteType.CHICANE
            token.startsWith("meta") || token.startsWith("finish") -> NoteType.FINISH
            token.startsWith("salida") || token.startsWith("start") -> NoteType.START
            else -> NoteType.CAUTION
        }
    }

    private fun defaultSeverity(type: NoteType): Int = when (type) {
        NoteType.HAIRPIN_LEFT, NoteType.HAIRPIN_RIGHT -> 1
        NoteType.LEFT, NoteType.RIGHT -> 4
        else -> 0
    }
}
