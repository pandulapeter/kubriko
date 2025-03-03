/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.permanent_marker
import org.jetbrains.compose.resources.Font

@Composable
internal fun BlockysJourneyTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xff456385),
        onPrimary = Color.White,
    ),
    typography = BlockysJourneyTypography(),
    shapes = Shapes(
        extraSmall = BlockysJourneyUIElementShape,
        small = BlockysJourneyUIElementShape,
        medium = BlockysJourneyUIElementShape,
        large = BlockysJourneyUIElementShape,
        extraLarge = BlockysJourneyUIElementShape,
    ),
) {
    CompositionLocalProvider(
        LocalIndication provides ripple(color = Color.White),
        LocalRippleConfiguration provides RippleConfiguration(
            color = Color.White,
            rippleAlpha = RippleAlpha(0f, 0f, 0f, 0.2f),
        )
    ) {
        content()
    }
}


internal val BlockysJourneyUIElementShape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(32.dp),
    topEnd = CornerSize(32.dp),
    bottomStart = CornerSize(32.dp),
    bottomEnd = CornerSize(32.dp),
)

@Composable
private fun BlockysJourneyTypography() = Typography().run {
    val fontFamily = PermanentMarkerFontFamily()
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
private fun PermanentMarkerFontFamily() = FontFamily(
    Font(Res.font.permanent_marker)
)
