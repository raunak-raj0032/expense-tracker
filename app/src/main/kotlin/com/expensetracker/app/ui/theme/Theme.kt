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
    primary = Color(0xFF19B58F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F5EB),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF2F7D73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0EFE9),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFFF4B860),
    onTertiary = Color(0xFF342100),
    tertiaryContainer = Color(0xFFFFE5BC),
    onTertiaryContainer = Color(0xFF4A3200),
    error = Color(0xFFE46A6A),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9D8),
    onErrorContainer = Color(0xFF4A1313),
    background = Color(0xFFF7FBFA),
    onBackground = Color(0xFF14201D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14201D),
    surfaceVariant = Color(0xFFDEE9E5),
    onSurfaceVariant = Color(0xFF58706A),
    outline = Color(0xFF93AAA4)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CD6B5),
    onPrimary = Color(0xFF03261D),
    primaryContainer = Color(0xFF103A31),
    onPrimaryContainer = Color(0xFFD0F7EB),
    secondary = Color(0xFF8BD8CA),
    onSecondary = Color(0xFF0D2520),
    secondaryContainer = Color(0xFF1B3732),
    onSecondaryContainer = Color(0xFFD5F3EE),
    tertiary = Color(0xFFF2C97D),
    onTertiary = Color(0xFF382700),
    tertiaryContainer = Color(0xFF524015),
    onTertiaryContainer = Color(0xFFFFE8BF),
    error = Color(0xFFFF8C8C),
    onError = Color(0xFF4A1313),
    errorContainer = Color(0xFF6A2525),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF101715),
    onBackground = Color(0xFFE8F3EF),
    surface = Color(0xFF171F1D),
    onSurface = Color(0xFFE8F3EF),
    surfaceVariant = Color(0xFF1F2B28),
    onSurfaceVariant = Color(0xFFADC3BD),
    outline = Color(0xFF455A55)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
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
