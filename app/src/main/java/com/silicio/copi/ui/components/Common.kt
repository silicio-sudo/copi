package com.silicio.copi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silicio.copi.BuildConfig
import com.silicio.copi.core.isOnline
import com.silicio.copi.data.NoteType
import com.silicio.copi.data.PaceNote
import com.silicio.copi.data.shortEs
import com.silicio.copi.nav.RallyState
import com.silicio.copi.ui.LocalBrand
import com.silicio.copi.ui.LiveryStyle
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun LiveryBackdrop(modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(brand.bg, brand.surface.copy(alpha = 0.96f), brand.bg)
            )
        )
        if (brand.livery == LiveryStyle.RALLYE) {
            rotate(degrees = -19f, pivot = Offset(size.width * 0.78f, size.height * 0.18f)) {
                drawRect(
                    color = brand.primary.copy(alpha = 0.13f),
                    topLeft = Offset(size.width * 0.58f, -size.height * 0.4f),
                    size = Size(size.width * 0.16f, size.height * 1.8f)
                )
                drawRect(
                    color = brand.accent.copy(alpha = 0.08f),
                    topLeft = Offset(size.width * 0.77f, -size.height * 0.4f),
                    size = Size(size.width * 0.035f, size.height * 1.8f)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.05f),
                    topLeft = Offset(size.width * 0.52f, -size.height * 0.4f),
                    size = Size(size.width * 0.02f, size.height * 1.8f)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(brand.primary.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.92f, size.height * 0.05f)
            )
        } else {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(brand.primary.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.90f, size.height * 0.05f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.90f, size.height * 0.05f)
            )
            for (i in 1..14) {
                drawLine(
                    color = brand.line.copy(alpha = 0.35f),
                    start = Offset(0f, size.height * i / 14f),
                    end = Offset(size.width, size.height * i / 14f),
                    strokeWidth = 1f
                )
            }
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, brand.bg.copy(alpha = 0.75f), brand.bg),
                startY = size.height * 0.55f,
                endY = size.height
            )
        )
    }
}

@Composable
fun BrandHeader(state: RallyState) {
    val brand = LocalBrand.current
    val online = isOnline(LocalContext.current)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WordmarkMark()
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = brand.wordmark,
                    style = MaterialTheme.typography.headlineLarge,
                    color = brand.text,
                    letterSpacing = 1.sp
                )
                Text(
                    text = brand.edition,
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.primary,
                    letterSpacing = 2.6.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(
                    text = when {
                        state.demo -> "DEMO"
                        state.gpsFix -> "GPS OK"
                        else -> "SIN GPS"
                    },
                    color = when {
                        state.demo -> brand.accent
                        state.gpsFix -> brand.ok
                        else -> brand.danger
                    }
                )
                Text(
                    text = if (online) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (online) brand.muted else brand.accent,
                    letterSpacing = 1.8.sp
                )
            }
        }
        if (brand.showPersonalization && brand.driver.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .background(brand.primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "PILOTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.muted,
                        letterSpacing = 2.4.sp
                    )
                    Text(
                        brand.driver,
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black,
                        color = brand.text,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "ART RALLYE",
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.muted.copy(alpha = 0.8f),
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

@Composable
fun WordmarkMark(size: Int = 38) {
    val brand = LocalBrand.current
    Canvas(Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = brand.surfaceAlt,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f),
            size = Size(w, h)
        )
        rotate(-22f, pivot = Offset(w / 2, h / 2)) {
            drawRect(
                color = brand.primary,
                topLeft = Offset(w * 0.18f, h * 0.10f),
                size = Size(w * 0.22f, h * 0.80f)
            )
            drawRect(
                color = brand.accent,
                topLeft = Offset(w * 0.46f, h * 0.10f),
                size = Size(w * 0.055f, h * 0.80f)
            )
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun BrandFooter(modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(brand.line)
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${brand.wordmark} · v${BuildConfig.VERSION_NAME.removeSuffix("-debug")}",
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.weight(1f))
            if (brand.showPersonalization && brand.handle.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = brand.url.isNotBlank()) {
                            if (brand.url.isNotBlank()) uriHandler.openUri(brand.url)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = "GitHub",
                        tint = brand.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = brand.handle,
                        style = MaterialTheme.typography.labelSmall,
                        color = brand.accent,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = if (brand.showPersonalization) brand.tagline else "Copiloto de rally · offline y online",
            style = MaterialTheme.typography.bodySmall,
            color = brand.muted.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CopiCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val brand = LocalBrand.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = brand.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, if (highlight) brand.primary.copy(alpha = 0.55f) else brand.line),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    val brand = LocalBrand.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(Locale("es", "ES")),
            style = MaterialTheme.typography.labelSmall,
            color = brand.muted,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelSmall,
                color = brand.muted.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatCell(label: String, value: String, unit: String? = null) {
    val brand = LocalBrand.current
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = brand.text,
                fontStyle = FontStyle.Italic
            )
            if (unit != null) {
                Text(
                    " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.muted
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = brand.muted,
            letterSpacing = 1.2.sp
        )
    }
}

fun noteIcon(type: NoteType): ImageVector = when (type) {
    NoteType.LEFT, NoteType.HAIRPIN_LEFT, NoteType.KEEP_LEFT -> Icons.Filled.TurnLeft
    NoteType.RIGHT, NoteType.HAIRPIN_RIGHT, NoteType.KEEP_RIGHT -> Icons.Filled.TurnRight
    NoteType.STRAIGHT -> Icons.Filled.ArrowUpward
    NoteType.START -> Icons.Filled.PlayArrow
    NoteType.FINISH -> Icons.Filled.Flag
    NoteType.CREST -> Icons.Filled.Terrain
    NoteType.JUMP, NoteType.CAUTION, NoteType.CHICANE -> Icons.Filled.Warning
}

@Composable
fun NoteTypeIcon(note: PaceNote, modifier: Modifier = Modifier, size: Int = 26) {
    val brand = LocalBrand.current
    val tint = when (note.type) {
        NoteType.LEFT, NoteType.RIGHT, NoteType.HAIRPIN_LEFT, NoteType.HAIRPIN_RIGHT -> brand.primary
        NoteType.FINISH -> brand.ok
        NoteType.CAUTION, NoteType.JUMP -> brand.danger
        else -> brand.accent
    }
    Icon(
        imageVector = noteIcon(note.type),
        contentDescription = note.shortEs(),
        tint = tint,
        modifier = modifier.size(size.dp)
    )
}

@Composable
fun EmptyHint(title: String, text: String, icon: ImageVector? = null) {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(brand.surfaceAlt.copy(alpha = 0.6f))
            .border(1.dp, brand.line, RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = brand.primary, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = brand.text)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = brand.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}
