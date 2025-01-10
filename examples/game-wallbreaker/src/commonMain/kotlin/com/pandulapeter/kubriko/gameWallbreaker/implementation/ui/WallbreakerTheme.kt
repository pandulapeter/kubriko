package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

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
import com.pandulapeter.kubriko.uiComponents.utilities.isFontFamilyLoaded
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.kanit_regular
import org.jetbrains.compose.resources.Font

@Composable
internal fun WallbreakerTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = darkColorScheme(
        primaryContainer = Color(0xcfd5e3bf),
        primary = Color(0xcfd5e3bf),
        onPrimary = Color.Black,
    ),
    typography = WallbreakerTypography(),
    shapes = Shapes(
        extraSmall = Shape,
        small = Shape,
        medium = Shape,
        large = Shape,
        extraLarge = Shape,
    ),
    content = content
)

private val Shape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(0),
    topEnd = CornerSize(0),
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(0),
)

internal fun createButtonColor(hue: Float) = Color.hsv(hue * 360, 0.3f, 1f)

@Composable
private fun WallbreakerTypography() = Typography().run {
    val fontFamily = KanitRegularFontFamily()
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
internal fun isWallbreakerFontLoaded() = isFontFamilyLoaded(KanitRegularFontFamily())

@Composable
private fun KanitRegularFontFamily() = FontFamily(
    Font(Res.font.kanit_regular, weight = FontWeight.Normal),
)