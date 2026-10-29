/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.public_sans_bold
import kubriko.creator.generated.resources.public_sans_medium
import kubriko.creator.generated.resources.public_sans_regular
import kubriko.creator.generated.resources.public_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Material's type scale resized for the Creator: the sizes are larger than Material's because the whole interface is drawn
 * at a reduced density (see ProvideInterfaceScale), and titles and labels get real weights, so headers stand out from
 * the text under them.
 */
@Composable
internal fun CreatorTypography(): Typography {
    val fontFamily = FontFamily(
        Font(Res.font.public_sans_regular, FontWeight.Normal),
        Font(Res.font.public_sans_medium, FontWeight.Medium),
        Font(Res.font.public_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.public_sans_bold, FontWeight.Bold),
    )
    return Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = fontFamily),
            displayMedium = displayMedium.copy(fontFamily = fontFamily),
            displaySmall = displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = headlineLarge.resized(fontFamily, 34.sp, 42.sp, FontWeight.SemiBold),
            headlineMedium = headlineMedium.resized(fontFamily, 30.sp, 38.sp, FontWeight.SemiBold),
            headlineSmall = headlineSmall.resized(fontFamily, 26.sp, 34.sp, FontWeight.SemiBold),
            titleLarge = titleLarge.resized(fontFamily, 21.sp, 28.sp, FontWeight.SemiBold),
            titleMedium = titleMedium.resized(fontFamily, 17.sp, 24.sp, FontWeight.SemiBold),
            titleSmall = titleSmall.resized(fontFamily, 15.sp, 21.sp, FontWeight.SemiBold),
            bodyLarge = bodyLarge.resized(fontFamily, 17.sp, 24.sp, FontWeight.Normal),
            bodyMedium = bodyMedium.resized(fontFamily, 15.sp, 21.sp, FontWeight.Normal),
            bodySmall = bodySmall.resized(fontFamily, 14.sp, 19.sp, FontWeight.Normal),
            labelLarge = labelLarge.resized(fontFamily, 15.sp, 21.sp, FontWeight.Medium),
            labelMedium = labelMedium.resized(fontFamily, 14.sp, 19.sp, FontWeight.Medium),
            labelSmall = labelSmall.resized(fontFamily, 13.sp, 18.sp, FontWeight.Medium),
        )
    }
}

private fun TextStyle.resized(
    fontFamily: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
) = copy(
    fontFamily = fontFamily,
    fontSize = fontSize,
    lineHeight = lineHeight,
    fontWeight = fontWeight,
)
