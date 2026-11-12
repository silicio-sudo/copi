package com.silicio.copi.data

import com.silicio.copi.data.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class Place(
    val name: String,
    val displayName: String,
    val lat: Double,
    val lon: Double
)

data class PlannedRoute(
    val points: List<GeoPoint>,
    val distanceM: Double,
    val durationS: Double
)

/**
 * Planificación online opcional: Nominatim (búsqueda) + OSRM (ruta).
 * Los servidores públicos son de demostración; para uso intensivo configura
 * tu propia instancia en Ajustes → Avanzado.
 */
class RoutePlanner(private val settings: () -> AppSettings) {

    suspend fun geocode(query: String): List<Place> = withContext(Dispatchers.IO) {
        val base = settings().nominatimBaseUrl.trimEnd('/')
        val url = "$base/search?format=jsonv2&limit=5&accept-language=es&q=" +
            URLEncoder.encode(query, "UTF-8")
        val response = httpGet(url)
        val array = JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                val display = obj.optString("display_name")
                val name = obj.optString("name").ifBlank { display.substringBefore(",") }
                add(Place(name, display, lat, lon))
            }
        }
    }

    suspend fun route(from: GeoPoint, to: GeoPoint): PlannedRoute = withContext(Dispatchers.IO) {
        val base = settings().osrmBaseUrl.trimEnd('/')
        val coords = String.format(
            Locale.US,
            "%.6f,%.6f;%.6f,%.6f",
            from.lon, from.lat, to.lon, to.lat
        )
        val url = "$base/route/v1/driving/$coords?overview=full&geometries=geojson&steps=false"
        val response = httpGet(url)
        val root = JSONObject(response)
        val code = root.optString("code")
        if (code != "Ok") {
            val message = root.optString("message").ifBlank { code }
            throw IOException("OSRM: $message")
        }
        val routes = root.optJSONArray("routes") ?: throw IOException("OSRM no devolvió rutas")
        if (routes.length() == 0) throw IOException("No se encontró una ruta por carretera")
        val route = routes.getJSONObject(0)
        val distance = route.optDouble("distance", 0.0)
        val duration = route.optDouble("duration", 0.0)
        val coordsArray = route.getJSONObject("geometry").getJSONArray("coordinates")
        val points = ArrayList<GeoPoint>(coordsArray.length())
        for (i in 0 until coordsArray.length()) {
            val pair = coordsArray.getJSONArray(i)
            val lon = pair.getDouble(0)
            val lat = pair.getDouble(1)
            points.add(GeoPoint(lat, lon))
        }
        if (points.size < 2) throw IOException("La ruta recibida no tiene suficientes puntos")
        PlannedRoute(points, distance, duration)
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty(
            "User-Agent",
            "Copi/1.0 (Android; +https://github.com/silicio-sudo)"
        )
        conn.setRequestProperty("Accept", "application/json")
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(300)
                throw IOException("HTTP $code ${error.orEmpty()}".trim())
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private val COORDINATES = Regex("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*[,;]\\s*(-?\\d+(?:\\.\\d+)?)\\s*$")

        fun parseCoordinates(query: String): Place? {
            val match = COORDINATES.matchEntire(query) ?: return null
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lon = match.groupValues[2].toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
            return Place(
                name = String.format(Locale.US, "%.5f, %.5f", lat, lon),
                displayName = "Coordenadas",
                lat = lat,
                lon = lon
            )
        }
    }
}
