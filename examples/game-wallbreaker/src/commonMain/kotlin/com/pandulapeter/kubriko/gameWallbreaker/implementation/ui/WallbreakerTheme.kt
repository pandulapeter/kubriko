/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.kanit_regular
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WallbreakerTheme(
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = darkColorScheme(
        primaryContainer = Color(0xcfd5e3bf),
        primary = Color(0xcfd5e3bf),
        onPrimary = Color.Black,
    ),
    typography = Typography().withFontFamily(KanitRegularFontFamily()),
    shapes = Shapes(
        extraSmall = Shape,
        small = Shape,
        medium = Shape,
        large = Shape,
        extraLarge = Shape,
    ),
) {
    CompositionLocalProvider(
        LocalIndication provides gameRipple(
            color = Color.Black,
            rippleAlpha = RippleAlpha(0.2f, 0.2f, 0.2f, 0.2f),
        ),
        LocalRippleConfiguration provides RippleConfiguration(Color.Black),
    ) {
        content()
    }
}

private val Shape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(0),
    topEnd = CornerSize(0),
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(0),
)

@Composable
private fun KanitRegularFontFamily() = FontFamily(
    Font(Res.font.kanit_regular)
)