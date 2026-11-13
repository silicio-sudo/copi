package com.silicio.copi.ui.components

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.silicio.copi.data.GeoPoint
import com.silicio.copi.ui.LocalBrand
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max

/**
 * Mapa real con teselas de OpenStreetMap. Las teselas se cachean en el
 * almacenamiento de la app; la ruta y la posición se dibujan siempre, incluso
 * sin conexión. Atribución: © OpenStreetMap contributors.
 */
@Composable
fun RallyMap(
    points: List<GeoPoint>,
    current: GeoPoint?,
    followCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val brand = LocalBrand.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setTilesScaledToDpi(true)
            setBackgroundColor(Color.TRANSPARENT)
            controller.setZoom(15.0)
        }
    }
    val routeOverlay = remember(mapView) {
        Polyline(mapView).apply {
            outlinePaint.color = brand.primary.toArgb()
            outlinePaint.strokeWidth = 9f
            outlinePaint.isAntiAlias = true
        }
    }
    val currentOverlay = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Tu posición"
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier.fillMaxSize().background(brand.bg)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { it.invalidate() }
        )
    }

    LaunchedEffect(points) {
        if (points.size < 2) {
            mapView.overlays.remove(routeOverlay)
            mapView.invalidate()
            return@LaunchedEffect
        }
        routeOverlay.setPoints(points.map { OsmGeoPoint(it.lat, it.lon) })
        if (!mapView.overlays.contains(routeOverlay)) {
            mapView.overlays.add(0, routeOverlay)
        }
        val north = points.maxOf { it.lat }
        val south = points.minOf { it.lat }
        val east = points.maxOf { it.lon }
        val west = points.minOf { it.lon }
        mapView.zoomToBoundingBox(
            BoundingBox(
                north + 0.0004,
                east + 0.0004,
                south - 0.0004,
                west - 0.0004
            ),
            true,
            90
        )
        mapView.invalidate()
    }

    LaunchedEffect(current, followCurrent) {
        val position = current ?: return@LaunchedEffect
        currentOverlay.position = OsmGeoPoint(position.lat, position.lon)
        if (!mapView.overlays.contains(currentOverlay)) {
            mapView.overlays.add(currentOverlay)
        }
        if (followCurrent) {
            mapView.controller.setCenter(currentOverlay.position)
            mapView.controller.setZoom(max(16.0, mapView.zoomLevelDouble))
        }
        mapView.invalidate()
    }
}
