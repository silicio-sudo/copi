package com.silicio.copi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun CopiTheme(
    brand: Brand = rememberBrand(),
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = brand.primary,
        onPrimary = brand.onPrimary,
        primaryContainer = brand.surfaceAlt,
        onPrimaryContainer = brand.text,
        secondary = brand.accent,
        onSecondary = brand.bg,
        background = brand.bg,
        onBackground = brand.text,
        surface = brand.surface,
        onSurface = brand.text,
        surfaceVariant = brand.surfaceAlt,
        onSurfaceVariant = brand.muted,
        outline = brand.line,
        outlineVariant = brand.line,
        error = brand.danger,
        onError = brand.onPrimary
    )

    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 64.sp,
            letterSpacing = (-2).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 42.sp,
            letterSpacing = (-1).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
            letterSpacing = 0.5.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.6.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    )

    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content
        )
    }
}
