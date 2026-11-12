package com.silicio.copi.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RoadbookRepository(context: Context) {
    private val appContext = context.applicationContext

    private val directory: File
        get() = File(appContext.filesDir, "roadbooks").apply { mkdirs() }

    suspend fun list(): List<Roadbook> = withContext(Dispatchers.IO) {
        directory.listFiles { file -> file.isFile && file.extension == "json" }
            ?.mapNotNull { file -> runCatching { RoadbookCodec.decode(file.readText()) }.getOrNull() }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun get(id: String): Roadbook? = withContext(Dispatchers.IO) {
        val file = fileFor(id)
        if (!file.exists()) return@withContext null
        runCatching { RoadbookCodec.decode(file.readText()) }.getOrNull()
    }

    suspend fun save(roadbook: Roadbook): Roadbook = withContext(Dispatchers.IO) {
        val file = fileFor(roadbook.id)
        val temp = File(directory, "${roadbook.id}.tmp")
        temp.writeText(RoadbookCodec.encode(roadbook))
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            file.writeText(temp.readText())
            temp.delete()
        }
        roadbook
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        fileFor(id).delete()
    }

    suspend fun importGpx(displayName: String, input: java.io.InputStream): Roadbook {
        val gpx = withContext(Dispatchers.IO) { GpxCodec.parse(input) }
        require(gpx.points.size >= 2) { "El GPX no contiene suficientes puntos de ruta" }
        val name = gpx.name?.takeIf { it.isNotBlank() } ?: displayName
        val roadbook = PacenoteGenerator.fromRoute(
            points = gpx.points,
            name = name.take(80),
            source = "gpx",
            id = UUID.randomUUID().toString()
        )
        return save(roadbook)
    }

    suspend fun importCsv(displayName: String, text: String): Roadbook {
        val roadbook = withContext(Dispatchers.IO) { RoadbookCodec.parseCsv(text, displayName) }
        return save(roadbook)
    }

    suspend fun seedDemoIfEmpty(): Roadbook? = withContext(Dispatchers.IO) {
        val existing = list()
        if (existing.isNotEmpty()) return@withContext existing.firstOrNull()
        val demo = DemoRoute.roadbook()
        save(demo)
    }

    fun exportCsv(roadbook: Roadbook): String = RoadbookCodec.toCsv(roadbook)

    private fun fileFor(id: String): File {
        val safe = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(directory, "$safe.json")
    }
}
