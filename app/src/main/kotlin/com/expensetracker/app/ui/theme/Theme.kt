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
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.expensetracker.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── Palette ─────────────────────────────────────────────────────────────────
// Electric Midnight — deep blacks, electric mint, neon cyan, amber spark
object AppColors {
    // Mint / Primary
    val Mint400 = Color(0xFF00E5A0)        // electric mint — hero CTA
    val Mint300 = Color(0xFF4DFFC4)        // lighter glow
    val Mint900 = Color(0xFF003D28)        // deep container

    // Cyan / Secondary
    val Cyan400 = Color(0xFF00D4F5)        // neon cyan
    val Cyan200 = Color(0xFF80ECFC)
    val Cyan900 = Color(0xFF002B35)

    // Amber / Tertiary
    val Amber400 = Color(0xFFFFB830)       // amber spark
    val Amber200 = Color(0xFFFFD980)
    val Amber900 = Color(0xFF3D2A00)

    // Coral / Error
    val Coral400 = Color(0xFFFF5A72)
    val Coral200 = Color(0xFFFF9BAA)
    val Coral900 = Color(0xFF4A0012)

    // Violet accent (subtle depth)
    val Violet400 = Color(0xFFA855F7)

    // Dark backgrounds (rich blacks with slight green tint)
    val Ink950  = Color(0xFF080F0D)        // deepest background
    val Ink900  = Color(0xFF0D1714)        // surface
    val Ink800  = Color(0xFF142018)        // elevated surface
    val Ink700  = Color(0xFF1C2E27)        // surface variant
    val Ink600  = Color(0xFF263D34)        // outline variant

    // Light mode (clean off-white)
    val Cream50  = Color(0xFFF5FDF9)
    val Cream100 = Color(0xFFE8F8F1)
    val Ink100   = Color(0xFF1A3028)       // on-background light
    val Ink200   = Color(0xFF2D4A3D)
    val Ink300   = Color(0xFF406356)
    val Outline  = Color(0xFFB8D8CC)
}

private val DarkColorScheme = darkColorScheme(
    primary             = AppColors.Mint400,
    onPrimary           = AppColors.Ink950,
    primaryContainer    = AppColors.Mint900,
    onPrimaryContainer  = AppColors.Mint300,

    secondary           = AppColors.Cyan400,
    onSecondary         = AppColors.Ink950,
    secondaryContainer  = AppColors.Cyan900,
    onSecondaryContainer= AppColors.Cyan200,

    tertiary            = AppColors.Amber400,
    onTertiary          = AppColors.Ink950,
    tertiaryContainer   = AppColors.Amber900,
    onTertiaryContainer = AppColors.Amber200,

    error               = AppColors.Coral400,
    onError             = AppColors.Ink950,
    errorContainer      = AppColors.Coral900,
    onErrorContainer    = AppColors.Coral200,

    background          = AppColors.Ink950,
    onBackground        = Color(0xFFE8F5F0),
    surface             = AppColors.Ink900,
    onSurface           = Color(0xFFDFF2EA),
    surfaceVariant      = AppColors.Ink800,
    onSurfaceVariant    = Color(0xFF8BAAA0),
    outline             = AppColors.Ink600
)

private val LightColorScheme = lightColorScheme(
    primary             = Color(0xFF00A874),
    onPrimary           = Color.White,
    primaryContainer    = AppColors.Cream100,
    onPrimaryContainer  = AppColors.Ink100,

    secondary           = Color(0xFF0096B4),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFD0F5FC),
    onSecondaryContainer= AppColors.Ink200,

    tertiary            = Color(0xFFE09800),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFFF0CC),
    onTertiaryContainer = AppColors.Ink200,

    error               = Color(0xFFD93251),
    onError             = Color.White,
    errorContainer      = Color(0xFFFFE0E6),
    onErrorContainer    = Color(0xFF4A0012),

    background          = AppColors.Cream50,
    onBackground        = AppColors.Ink100,
    surface             = Color.White,
    onSurface           = AppColors.Ink100,
    surfaceVariant      = AppColors.Cream100,
    onSurfaceVariant    = AppColors.Ink300,
    outline             = AppColors.Outline
)

// ─── Typography ───────────────────────────────────────────────────────────────
// DM Sans via Google Fonts Downloadable Fonts API. Falls back to system sans-serif
// if the GMS provider is unavailable (e.g. no Play Services).
private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val DmSansFont = GoogleFont("DM Sans")

private val DmSans = FontFamily(
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = DmSansFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Black)
)

// Bold, tight, high-contrast financial typography
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Black,
        fontSize      = 48.sp,
        lineHeight    = 52.sp,
        letterSpacing = (-2.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 38.sp,
        lineHeight    = 42.sp,
        letterSpacing = (-1.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 30.sp,
        lineHeight    = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 26.sp,
        lineHeight    = 32.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 19.sp,
        lineHeight    = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 16.sp,
        lineHeight    = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = DmSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        lineHeight    = 13.sp,
        letterSpacing = 0.6.sp
    )
)

// ─── Shapes ───────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

// ─── Theme ────────────────────────────────────────────────────────────────────
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
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            val lightBars = colorScheme.background.luminance() > 0.45f
            insetsController.isAppearanceLightStatusBars     = lightBars
            insetsController.isAppearanceLightNavigationBars = lightBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
