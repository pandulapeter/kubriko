package com.pandulapeter.kubriko.uiComponents.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import kubriko.tools.ui_components.generated.resources.Res
import kubriko.tools.ui_components.generated.resources.public_sans_regular
import org.jetbrains.compose.resources.Font

@Composable
internal fun KubrikoTypography() = Typography().run {
    val fontFamily = PublicSansRegularFontFamily()
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
internal fun isKubrikoFontLoaded() = true // TODO: preloadedFont(Res.font.public_sans_regular).value != null

@Composable
private fun PublicSansRegularFontFamily() = FontFamily(
    Font(Res.font.public_sans_regular)
)