package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.orbitron
import org.jetbrains.compose.resources.Font

@Composable
internal fun SpaceSquadronTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = darkColorScheme(
        primaryContainer = Color(0xcfe1eaf3),
        primary = Color(0xcfe1eaf3),
        onPrimary = Color.Black,
    ),
    typography = SpaceSquadronTypography(),
    shapes = Shapes(
        extraSmall = Shape,
        small = Shape,
        medium = Shape,
        large = Shape,
        extraLarge = Shape,
    ),
    content = content
)

@Composable
internal fun isFontLoaded() = SpaceSquadronTypography().bodyMedium.fontSize > 1.sp

private val Shape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(50),
    topEnd = CornerSize(0),
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(50),
)

@Composable
private fun SpaceSquadronTypography() = Typography().run {
    val fontFamily = OrbitronFontFamily()
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily)
    )
}

@Composable
private fun OrbitronFontFamily() = FontFamily(
    Font(Res.font.orbitron, weight = FontWeight.Normal),
)
