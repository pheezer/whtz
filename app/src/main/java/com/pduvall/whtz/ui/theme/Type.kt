package com.pduvall.whtz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.pduvall.whtz.R

// Fredoka ships as one variable font (wght 300–700); pin each weight via variation settings.
private fun fredoka(weight: FontWeight) = Font(
    resId = R.font.fredoka,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Fredoka = FontFamily(
    fredoka(FontWeight.Light),
    fredoka(FontWeight.Normal),
    fredoka(FontWeight.Medium),
    fredoka(FontWeight.SemiBold),
    fredoka(FontWeight.Bold),
)

private val default = Typography()
val WhtzTypography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = Fredoka),
    displayMedium = default.displayMedium.copy(fontFamily = Fredoka),
    displaySmall = default.displaySmall.copy(fontFamily = Fredoka),
    headlineLarge = default.headlineLarge.copy(fontFamily = Fredoka),
    headlineMedium = default.headlineMedium.copy(fontFamily = Fredoka),
    headlineSmall = default.headlineSmall.copy(fontFamily = Fredoka),
    titleLarge = default.titleLarge.copy(fontFamily = Fredoka),
    titleMedium = default.titleMedium.copy(fontFamily = Fredoka),
    titleSmall = default.titleSmall.copy(fontFamily = Fredoka),
    bodyLarge = default.bodyLarge.copy(fontFamily = Fredoka),
    bodyMedium = default.bodyMedium.copy(fontFamily = Fredoka),
    bodySmall = default.bodySmall.copy(fontFamily = Fredoka),
    labelLarge = default.labelLarge.copy(fontFamily = Fredoka),
    labelMedium = default.labelMedium.copy(fontFamily = Fredoka),
    labelSmall = default.labelSmall.copy(fontFamily = Fredoka),
)
