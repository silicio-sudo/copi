package com.silicio.copi.data

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val alt: Double? = null
)

@Serializable
enum class NoteType {
    START,
    LEFT,
    RIGHT,
    STRAIGHT,
    HAIRPIN_LEFT,
    HAIRPIN_RIGHT,
    KEEP_LEFT,
    KEEP_RIGHT,
    CREST,
    JUMP,
    CAUTION,
    CHICANE,
    FINISH
}

@Serializable
data class PaceNote(
    val distanceM: Double,
    val type: NoteType,
    val severity: Int = 0,
    val modifier: String = "",
    val instruction: String = "",
    val extra: String = ""
) {
    val isCorner: Boolean
        get() = type == NoteType.LEFT || type == NoteType.RIGHT ||
            type == NoteType.HAIRPIN_LEFT || type == NoteType.HAIRPIN_RIGHT
}

@Serializable
data class Roadbook(
    val id: String,
    val name: String,
    val createdAt: Long,
    val distanceM: Double,
    val notes: List<PaceNote>,
    val route: List<GeoPoint> = emptyList(),
    val source: String = "manual",
    val version: Int = 1
) {
    val sortedNotes: List<PaceNote>
        get() = notes.sortedBy { it.distanceM }
}

@Serializable
enum class UnitSystem { METRIC, IMPERIAL }

@Serializable
enum class VoiceMode { AUTO, LOCAL, GOOGLE, ELEVEN }

@Serializable
data class AppSettings(
    val units: UnitSystem = UnitSystem.METRIC,
    val voiceEnabled: Boolean = true,
    val voiceMode: VoiceMode = VoiceMode.AUTO,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.02f,
    val selectedVoice: String? = null,
    val lookaheadSeconds: Int = 6,
    val minTriggerM: Int = 80,
    val reminders: Boolean = true,
    val beepOnPass: Boolean = true,
    val keepScreenOn: Boolean = true,
    val demoMode: Boolean = false,
    val onlineServices: Boolean = true,
    val googleTtsApiKey: String = "",
    val googleVoice: String = "es-ES-Neural2-F",
    val elevenApiKey: String = "",
    val elevenVoiceId: String = "EXAVITQu4vr4xnSDxMaL",
    val osrmBaseUrl: String = "https://router.project-osrm.org",
    val nominatimBaseUrl: String = "https://nominatim.openstreetmap.org",
    val activeRoadbookId: String? = null
)
