package com.autodub.edge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CyberColors {
    val DarkNavy = Color(0xFF0A0F1E)
    val SurfaceNavy = Color(0xFF111827)
    val CardNavy = Color(0xFF161F33)
    val BorderSlate = Color(0xFF24304A)
    val CyberCyan = Color(0xFF00E5FF)
    val CyberPurple = Color(0xFFA855F7)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val EmeraldActive = Color(0xFF22C55E)
    val AmberWarn = Color(0xFFFACC15)
    val CrimsonError = Color(0xFFEF4444)
}

private val AutoDubDarkColorScheme = darkColorScheme(
    primary = CyberColors.CyberCyan,
    onPrimary = CyberColors.DarkNavy,
    secondary = CyberColors.CyberPurple,
    onSecondary = Color.White,
    tertiary = CyberColors.EmeraldActive,
    background = CyberColors.DarkNavy,
    onBackground = CyberColors.TextPrimary,
    surface = CyberColors.SurfaceNavy,
    onSurface = CyberColors.TextPrimary,
    surfaceVariant = CyberColors.CardNavy,
    onSurfaceVariant = CyberColors.TextSecondary,
    outline = CyberColors.BorderSlate,
    error = CyberColors.CrimsonError,
    onError = Color.White
)

private val AutoDubTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = CyberColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = CyberColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = CyberColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = CyberColors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = CyberColors.TextSecondary
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = CyberColors.TextSecondary
    )
)

@Composable
fun AutoDubEdgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AutoDubDarkColorScheme,
        typography = AutoDubTypography,
        content = content
    )
}
