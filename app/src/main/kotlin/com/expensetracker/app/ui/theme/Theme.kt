package com.expensetracker.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2CC58D),
    onPrimary = Color(0xFF063122),
    primaryContainer = Color(0xFFCFF7E8),
    onPrimaryContainer = Color(0xFF053021),
    secondary = Color(0xFF0FA3B1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F6F9),
    onSecondaryContainer = Color(0xFF06363B),
    tertiary = Color(0xFFFFC84A),
    onTertiary = Color(0xFF4B3200),
    tertiaryContainer = Color(0xFFFFEDB8),
    onTertiaryContainer = Color(0xFF4A3000),
    error = Color(0xFFE85D75),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9E0),
    onErrorContainer = Color(0xFF561722),
    background = Color(0xFFFFFCF4),
    onBackground = Color(0xFF17302B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17302B),
    surfaceVariant = Color(0xFFF1F7F2),
    onSurfaceVariant = Color(0xFF58716A),
    outline = Color(0xFFC7DDD1)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5BE0AC),
    onPrimary = Color(0xFF05261A),
    primaryContainer = Color(0xFF103C2C),
    onPrimaryContainer = Color(0xFFD7FAEA),
    secondary = Color(0xFF67D7E3),
    onSecondary = Color(0xFF082B30),
    secondaryContainer = Color(0xFF123D43),
    onSecondaryContainer = Color(0xFFD2F7FA),
    tertiary = Color(0xFFFFCF62),
    onTertiary = Color(0xFF453000),
    tertiaryContainer = Color(0xFF5A420B),
    onTertiaryContainer = Color(0xFFFFEDBA),
    error = Color(0xFFFF8FA2),
    onError = Color(0xFF561722),
    errorContainer = Color(0xFF6D2634),
    onErrorContainer = Color(0xFFFFD9E0),
    background = Color(0xFF0E1816),
    onBackground = Color(0xFFEAF5F0),
    surface = Color(0xFF172321),
    onSurface = Color(0xFFEAF5F0),
    surfaceVariant = Color(0xFF22302D),
    onSurfaceVariant = Color(0xFFA8C3BA),
    outline = Color(0xFF49635D)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            val lightBars = colorScheme.background.luminance() > 0.45f
            insetsController.isAppearanceLightStatusBars = lightBars
            insetsController.isAppearanceLightNavigationBars = lightBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
