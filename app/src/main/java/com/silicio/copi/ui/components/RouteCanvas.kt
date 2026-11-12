package com.silicio.copi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.silicio.copi.core.RoutePath
import com.silicio.copi.data.GeoPoint
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PaceNote
import com.silicio.copi.ui.LocalBrand
import kotlin.math.cos
import kotlin.math.min

@Composable
fun RouteCanvas(
    points: List<GeoPoint>,
    notes: List<PaceNote> = emptyList(),
    current: GeoPoint? = null,
    highlightIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    val brand = LocalBrand.current
    val pathModel = remember(points) { RoutePath(points) }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(brand.surfaceAlt.copy(alpha = 0.85f))
            .border(1.dp, brand.line, shape)
    ) {
        if (points.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin trazado GPS\nImporta un GPX para dibujar la ruta",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val bounds = pathModel.bounds() ?: return@Canvas
                val pad = 8.dp.toPx()
                val midLat = (bounds.minLat + bounds.maxLat) / 2
                val cosMid = cos(Math.toRadians(midLat)).coerceAtLeast(0.1)
                val spanX = ((bounds.maxLon - bounds.minLon) * cosMid).coerceAtLeast(1e-7)
                val spanY = (bounds.maxLat - bounds.minLat).coerceAtLeast(1e-7)
                val scale = min(
                    (size.width - pad * 2) / spanX,
                    (size.height - pad * 2) / spanY
                )
                val centerLon = (bounds.minLon + bounds.maxLon) / 2
                val centerLat = (bounds.minLat + bounds.maxLat) / 2

                fun project(point: GeoPoint): Offset = Offset(
                    x = size.width / 2 + ((point.lon - centerLon) * cosMid * scale).toFloat(),
                    y = size.height / 2 - ((point.lat - centerLat) * scale).toFloat()
                )

                for (i in 1..4) {
                    drawLine(
                        color = brand.line.copy(alpha = 0.5f),
                        start = Offset(0f, size.height * i / 5f),
                        end = Offset(size.width, size.height * i / 5f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = brand.line.copy(alpha = 0.5f),
                        start = Offset(size.width * i / 5f, 0f),
                        end = Offset(size.width * i / 5f, size.height),
                        strokeWidth = 1f
                    )
                }

                val path = Path()
                points.forEachIndexed { index, point ->
                    val p = project(point)
                    if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = brand.primary.copy(alpha = 0.20f),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = brand.primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                notes.forEachIndexed { index, note ->
                    if (note.type == NoteType.START || note.type == NoteType.FINISH) return@forEachIndexed
                    val p = pathModel.pointAt(note.distanceM)?.first ?: return@forEachIndexed
                    val offset = project(p)
                    val highlighted = index == highlightIndex
                    val color = when (note.type) {
                        NoteType.LEFT, NoteType.HAIRPIN_LEFT -> brand.accent
                        NoteType.RIGHT, NoteType.HAIRPIN_RIGHT -> brand.primary
                        NoteType.CAUTION, NoteType.JUMP -> brand.danger
                        else -> brand.muted
                    }
                    if (highlighted) {
                        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 6.dp.toPx(), center = offset)
                        drawCircle(color = color, radius = 4.5.dp.toPx(), center = offset)
                    } else {
                        drawCircle(color = brand.bg, radius = 3.6.dp.toPx(), center = offset)
                        drawCircle(color = color, radius = 2.6.dp.toPx(), center = offset)
                    }
                }

                val start = project(points.first())
                drawCircle(color = brand.ok, radius = 5.dp.toPx(), center = start)
                val end = project(points.last())
                drawCircle(color = brand.bg, radius = 6.dp.toPx(), center = end)
                drawCircle(color = brand.text, radius = 4.dp.toPx(), center = end)

                current?.let {
                    val p = project(it)
                    drawCircle(color = brand.bg, radius = 7.dp.toPx(), center = p)
                    drawCircle(color = brand.accent, radius = 5.dp.toPx(), center = p)
                    drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = p)
                }
            }

            Text(
                "TRAZADO",
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
            Text(
                "${notes.size} REGISTROS",
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}
