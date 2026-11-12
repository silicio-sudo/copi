package com.silicio.copi.data

/**
 * Texto de la nota en español y en el orden clásico de rally:
 * "izquierda 4, larga", "horquilla derecha", "meta".
 */
fun PaceNote.phraseEs(): String {
    if (instruction.isNotBlank()) return instruction
    val core = when (type) {
        NoteType.START -> "salida"
        NoteType.FINISH -> "meta"
        NoteType.LEFT -> "izquierda" + severityText()
        NoteType.RIGHT -> "derecha" + severityText()
        NoteType.HAIRPIN_LEFT -> "horquilla izquierda"
        NoteType.HAIRPIN_RIGHT -> "horquilla derecha"
        NoteType.STRAIGHT -> "recto"
        NoteType.KEEP_LEFT -> "mantén la izquierda"
        NoteType.KEEP_RIGHT -> "mantén la derecha"
        NoteType.CREST -> "cresta"
        NoteType.JUMP -> "salto"
        NoteType.CAUTION -> "precaución"
        NoteType.CHICANE -> "chicane"
    }
    val mod = listOf(modifier, extra).filter { it.isNotBlank() }.joinToString(", ")
    return if (mod.isNotEmpty()) "$core, $mod" else core
}

private fun PaceNote.severityText(): String =
    if (severity in 1..6) " $severity" else ""

/** Etiqueta corta para el HUD/AA: "IZQ 4", "HORQ DER", "META". */
fun PaceNote.shortEs(): String = when (type) {
    NoteType.START -> "SALIDA"
    NoteType.FINISH -> "META"
    NoteType.LEFT -> "IZQ" + severityShort()
    NoteType.RIGHT -> "DER" + severityShort()
    NoteType.HAIRPIN_LEFT -> "HORQ IZQ"
    NoteType.HAIRPIN_RIGHT -> "HORQ DER"
    NoteType.STRAIGHT -> "RECTO"
    NoteType.KEEP_LEFT -> "MANTÉN IZQ"
    NoteType.KEEP_RIGHT -> "MANTÉN DER"
    NoteType.CREST -> "CRESTA"
    NoteType.JUMP -> "SALTO"
    NoteType.CAUTION -> "PRECAUCIÓN"
    NoteType.CHICANE -> "CHICANE"
}

private fun PaceNote.severityShort(): String =
    if (severity in 1..6) " $severity" else ""

/** Anuncio de voz con distancia: "En 120 metros, izquierda 4, larga". */
fun PaceNote.announcement(distanceToNoteM: Double, units: UnitSystem): String {
    val phrase = phraseEs()
    val d = distanceToNoteM.coerceAtLeast(0.0)
    return when {
        d <= 15 -> "Ahora, $phrase"
        else -> "En ${com.silicio.copi.core.formatDistanceSpoken(d, units)}, $phrase"
    }
}

fun String.forSpeech(): String = replace(" km/h", " kilómetros por hora")
    .replace(" m ", " metros ")
    .replace("—", "")
    .trim()
