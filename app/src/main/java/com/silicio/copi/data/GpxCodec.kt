package com.silicio.copi.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object GpxCodec {
    data class GpxData(val name: String?, val points: List<GeoPoint>)

    fun parse(input: InputStream): GpxData {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val points = mutableListOf<GeoPoint>()
        var trackName: String? = null
        var pendingLat: Double? = null
        var pendingLon: Double? = null
        var pendingAlt: Double? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name?.lowercase()?.substringAfter(':')) {
                    "trkpt", "rtept", "wpt" -> {
                        pendingLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        pendingLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        pendingAlt = null
                    }
                    "ele" -> {
                        val text = parser.nextText().trim()
                        if (pendingLat != null) pendingAlt = text.toDoubleOrNull()
                    }
                    "name" -> {
                        val text = parser.nextText().trim()
                        if (trackName.isNullOrBlank() && text.isNotBlank()) trackName = text
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                when (parser.name?.lowercase()?.substringAfter(':')) {
                    "trkpt", "rtept", "wpt" -> {
                        val lat = pendingLat
                        val lon = pendingLon
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            points.add(GeoPoint(lat, lon, pendingAlt))
                        }
                        pendingLat = null
                        pendingLon = null
                        pendingAlt = null
                    }
                }
            }
            event = parser.next()
        }
        return GpxData(trackName, points)
    }
}
