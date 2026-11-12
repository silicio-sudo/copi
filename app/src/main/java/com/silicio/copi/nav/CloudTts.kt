package com.silicio.copi.nav

import android.content.Context
import android.util.Base64
import com.silicio.copi.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class Speaker { LOCAL, GOOGLE, ELEVEN }

/**
 * Voces naturales en la nube (Google Cloud TTS / ElevenLabs) con caché en
 * disco. La caché permite repetir las notas sin conexión una vez sintetizadas.
 */
class CloudTts(private val context: Context) {

    private val cacheDir: File
        get() = File(context.cacheDir, "voice").apply { mkdirs() }

    fun cacheFile(text: String, settings: AppSettings, speaker: Speaker): File {
        val voice = when (speaker) {
            Speaker.GOOGLE -> settings.googleVoice
            Speaker.ELEVEN -> settings.elevenVoiceId
            Speaker.LOCAL -> "local"
        }
        val material = "${speaker.name}|$voice|${settings.speechRate}|${settings.pitch}|$text"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$hash.mp3")
    }

    suspend fun synthesize(text: String, settings: AppSettings, speaker: Speaker): File? =
        withContext(Dispatchers.IO) {
            if (speaker == Speaker.LOCAL) return@withContext null
            val target = cacheFile(text, settings, speaker)
            if (target.exists() && target.length() > 200) return@withContext target
            val bytes = when (speaker) {
                Speaker.GOOGLE -> googleTts(text, settings)
                Speaker.ELEVEN -> elevenTts(text, settings)
                Speaker.LOCAL -> null
            } ?: return@withContext null
            if (bytes.size < 200) return@withContext null
            val temp = File(cacheDir, "${target.name}.tmp")
            temp.writeBytes(bytes)
            if (!temp.renameTo(target)) {
                target.writeBytes(bytes)
                temp.delete()
            }
            target
        }

    private fun googleTts(text: String, settings: AppSettings): ByteArray? {
        val key = settings.googleTtsApiKey.trim()
        if (key.isBlank()) return null
        val voice = settings.googleVoice.ifBlank { "es-ES-Neural2-F" }
        val language = voice.split("-").take(2).joinToString("-").ifBlank { "es-ES" }
        val body = JSONObject().apply {
            put("input", JSONObject().put("text", text))
            put(
                "voice",
                JSONObject()
                    .put("languageCode", language)
                    .put("name", voice)
            )
            put(
                "audioConfig",
                JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", settings.speechRate.coerceIn(0.25f, 4.0f))
                    .put("pitch", 0.0)
                    .put("volumeGainDb", 0.0)
            )
        }.toString().toByteArray()

        val conn = openPost(
            "https://texttospeech.googleapis.com/v1/text:synthesize?key=${urlEncode(key)}",
            body,
            mapOf("Content-Type" to "application/json; charset=utf-8")
        )
        return try {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val audio = json.optString("audioContent", "")
            if (audio.isBlank()) null else Base64.decode(audio, Base64.DEFAULT)
        } finally {
            conn.disconnect()
        }
    }

    private fun elevenTts(text: String, settings: AppSettings): ByteArray? {
        val key = settings.elevenApiKey.trim()
        if (key.isBlank()) return null
        val voice = settings.elevenVoiceId.trim().ifBlank { "EXAVITQu4vr4xnSDxMaL" }
        val body = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_multilingual_v2")
            put(
                "voice_settings",
                JSONObject()
                    .put("stability", 0.45)
                    .put("similarity_boost", 0.75)
                    .put("style", 0.25)
                    .put("use_speaker_boost", true)
            )
        }.toString().toByteArray()
        val conn = openPost(
            "https://api.elevenlabs.io/v1/text-to-speech/${urlEncode(voice)}?output_format=mp3_44100_128",
            body,
            mapOf(
                "xi-api-key" to key,
                "Content-Type" to "application/json",
                "Accept" to "audio/mpeg"
            )
        )
        return try {
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun openPost(url: String, body: ByteArray, headers: Map<String, String>): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.doOutput = true
        conn.setRequestProperty("User-Agent", "Copi/1.0 (Android; github.com/silicio-sudo)")
        headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
        conn.outputStream.use { it.write(body) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val error = runCatching {
                conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(400)
            }.getOrNull()
            conn.disconnect()
            throw IOException("HTTP $code ${error.orEmpty()}".trim())
        }
        return conn
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun clearCache(): Int {
        val files = cacheDir.listFiles()?.size ?: 0
        cacheDir.listFiles()?.forEach { it.delete() }
        return files
    }

    fun cacheSizeBytes(): Long =
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
}
