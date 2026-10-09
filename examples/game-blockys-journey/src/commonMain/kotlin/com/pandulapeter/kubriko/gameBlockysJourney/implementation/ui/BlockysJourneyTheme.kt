/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.pandulapeter.kubriko.shared.ui.gameRipple
import com.pandulapeter.kubriko.shared.ui.withFontFamily
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.medieval_sharp
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockysJourneyTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Color(0xffb3af8d),
        onPrimary = Color(0xff29261a),
    ),
    typography = Typography().withFontFamily(MedievalSharpFontFamily()),
    shapes = Shapes(
        extraSmall = BlockysJourneyUIElementShape,
        small = BlockysJourneyUIElementShape,
        medium = BlockysJourneyUIElementShape,
        large = BlockysJourneyUIElementShape,
        extraLarge = BlockysJourneyUIElementShape,
    ),
) {
    CompositionLocalProvider(
        LocalIndication provides gameRipple(
            color = Color.Black,
            rippleAlpha = RippleAlpha(0f, 0f, 0f, 0.2f),
        ),
        LocalRippleConfiguration provides RippleConfiguration(Color.Black),
    ) {
        content()
    }
}

@Composable
private fun MedievalSharpFontFamily() = FontFamily(
    Font(Res.font.medieval_sharp)
)
