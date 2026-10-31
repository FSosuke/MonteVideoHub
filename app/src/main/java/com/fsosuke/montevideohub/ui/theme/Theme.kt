package com.fsosuke.montevideohub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.fsosuke.montevideohub.R

/** Hausschrift von montedioyamagata.jp: Noto Sans JP (SIL Open Font License). */
val NotoSansJp = FontFamily(
    Font(R.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(R.font.noto_sans_jp_bold, FontWeight.Bold),
    Font(R.font.noto_sans_jp_black, FontWeight.Black),
)

private fun TextStyle.noto() = copy(fontFamily = NotoSansJp)

private val MonteTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.noto(),
        displayMedium = displayMedium.noto(),
        displaySmall = displaySmall.noto(),
        headlineLarge = headlineLarge.noto(),
        headlineMedium = headlineMedium.noto(),
        headlineSmall = headlineSmall.noto(),
        titleLarge = titleLarge.noto(),
        titleMedium = titleMedium.noto(),
        titleSmall = titleSmall.noto(),
        bodyLarge = bodyLarge.noto(),
        bodyMedium = bodyMedium.noto(),
        bodySmall = bodySmall.noto(),
        labelLarge = labelLarge.noto(),
        labelMedium = labelMedium.noto(),
        labelSmall = labelSmall.noto(),
    )
}

// Montedio-Yamagata-Palette: Nachtblau, Vereinsblau, Gelb, Weiß
val Navy950 = Color(0xFF03061F)
val Navy900 = Color(0xFF050A2E)
val Navy800 = Color(0xFF0B1A5C)
val Navy700 = Color(0xFF16245F)
val MontedioBlue = Color(0xFF1E3CC8)
val MontedioBlueLight = Color(0xFF5B76FF)
val MontedioYellow = Color(0xFFFFDD00)
val Snow = Color(0xFFF5F7FF)
val Mist = Color(0xFFB9C3F0)
val YouTubeRed = Color(0xFFFF2D2D)

private val MontedioColors = darkColorScheme(
    primary = MontedioYellow,
    onPrimary = Navy900,
    primaryContainer = MontedioBlue,
    onPrimaryContainer = Snow,
    secondary = MontedioBlueLight,
    onSecondary = Snow,
    secondaryContainer = Navy700,
    onSecondaryContainer = Snow,
    background = Navy900,
    onBackground = Snow,
    surface = Navy800,
    onSurface = Snow,
    surfaceVariant = Navy700,
    onSurfaceVariant = Mist,
    surfaceContainer = Navy800,
    surfaceContainerHigh = Navy700,
    inverseSurface = Snow,
    inverseOnSurface = Navy900,
    outline = Color(0xFF3A4C9A),
    outlineVariant = Color(0xFF26346E),
)

@Composable
fun MonteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MontedioColors, typography = MonteTypography, content = content)
}
