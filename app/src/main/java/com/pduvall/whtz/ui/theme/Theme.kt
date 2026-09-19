package com.pduvall.whtz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WhtzColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    onPrimary = AppBackground,
    primaryContainer = DarkTeal,
    onPrimaryContainer = BrightAqua,
    secondary = DeepTeal,
    onSecondary = LightText,
    secondaryContainer = SurfaceHighlight,
    onSecondaryContainer = LightText,
    tertiary = BrassGold,
    onTertiary = AppBackground,
    tertiaryContainer = CoatBrown,
    onTertiaryContainer = BrightBrass,
    background = AppBackground,
    onBackground = LightText,
    surface = SurfaceBase,
    onSurface = LightText,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = MutedText,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceHighlight,
    outline = OutlineColor,
    outlineVariant = DarkTeal,
)

/** Whtz uses a single dark teal/brass palette regardless of the system light/dark setting. */
@Composable
fun WhtzTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WhtzColorScheme, typography = WhtzTypography, content = content)
}
