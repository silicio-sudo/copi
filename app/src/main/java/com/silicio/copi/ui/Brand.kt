package com.silicio.copi.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.silicio.copi.R

enum class LiveryStyle { RALLYE, CLEAN }

data class Brand(
    val wordmark: String,
    val name: String,
    val edition: String,
    val tagline: String,
    val driver: String,
    val handle: String,
    val url: String,
    val legal: String,
    val showPersonalization: Boolean,
    val livery: LiveryStyle,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val danger: Color,
    val ok: Color
) {
    companion object {
        fun from(context: Context): Brand {
            val res = context.resources
            return Brand(
                wordmark = "COPI",
                name = res.getString(R.string.app_name),
                edition = res.getString(R.string.brand_edition),
                tagline = res.getString(R.string.brand_tagline),
                driver = res.getString(R.string.driver_name),
                handle = res.getString(R.string.social_handle),
                url = res.getString(R.string.social_url),
                legal = res.getString(R.string.brand_legal),
                showPersonalization = res.getBoolean(R.bool.show_personalization),
                livery = if (res.getString(R.string.livery_style) == "rallye") {
                    LiveryStyle.RALLYE
                } else {
                    LiveryStyle.CLEAN
                },
                bg = Color(ContextCompat.getColor(context, R.color.brand_bg)),
                surface = Color(ContextCompat.getColor(context, R.color.brand_surface)),
                surfaceAlt = Color(ContextCompat.getColor(context, R.color.brand_surface_alt)),
                primary = Color(ContextCompat.getColor(context, R.color.brand_primary)),
                onPrimary = Color(ContextCompat.getColor(context, R.color.brand_on_primary)),
                accent = Color(ContextCompat.getColor(context, R.color.brand_accent)),
                text = Color(ContextCompat.getColor(context, R.color.brand_text)),
                muted = Color(ContextCompat.getColor(context, R.color.brand_muted)),
                line = Color(ContextCompat.getColor(context, R.color.brand_line)),
                danger = Color(ContextCompat.getColor(context, R.color.brand_danger)),
                ok = Color(ContextCompat.getColor(context, R.color.brand_ok))
            )
        }
    }
}

val LocalBrand = staticCompositionLocalOf<Brand> {
    error("LocalBrand no inicializado")
}

@Composable
fun rememberBrand(): Brand {
    val context = LocalContext.current
    return remember(context) { Brand.from(context) }
}
