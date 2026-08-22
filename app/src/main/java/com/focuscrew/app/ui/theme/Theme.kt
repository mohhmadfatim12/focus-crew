package com.focuscrew.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =========================================================================
// FocusCrew — Daylight Editorial Color Scheme (100% Light Theme Luxury)
// =========================================================================
private val DaylightColorScheme = lightColorScheme(
    primary = AntiqueHoneyBrass,
    onPrimary = Color.White,
    primaryContainer = AntiqueHoneyBrassContainer,
    onPrimaryContainer = OnAntiqueHoneyBrassContainer,
    secondary = BritishRacingSage,
    onSecondary = Color.White,
    secondaryContainer = BritishRacingSageContainer,
    onSecondaryContainer = OnBritishRacingSageContainer,
    tertiary = TerracottaClay,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = CashmereCream,
    onBackground = SoftEspressoInk,
    surface = CashmereCream,
    onSurface = SoftEspressoInk,
    surfaceVariant = WarmLinen,
    onSurfaceVariant = MutedCharcoalOlive,
    surfaceTint = AntiqueHoneyBrass,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor
)

@Composable
fun FocusCrewTheme(
    darkTheme: Boolean = false, // Enforce Light Theme Luxury as approved
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DaylightColorScheme,
        content = content
    )
}
