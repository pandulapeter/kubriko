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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Soft, slightly cool neutrals around Kubriko's brand color: text is never pure white or black, and the surfaces of
 * neighboring panels differ by a shade rather than a line, so long sessions stay easy on the eyes.
 */
internal val CreatorLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF5858A3),
    onPrimary = Color(0xFFFCFCFF),
    primaryContainer = Color(0xFFE2E2F6),
    onPrimaryContainer = Color(0xFF26265A),
    inversePrimary = Color(0xFFA9A9EC),
    secondary = Color(0xFF5E5F7A),
    onSecondary = Color(0xFFFCFCFF),
    secondaryContainer = Color(0xFFE4E4F0),
    onSecondaryContainer = Color(0xFF2C2D44),
    tertiary = Color(0xFF3D7F76),
    onTertiary = Color(0xFFFCFCFF),
    tertiaryContainer = Color(0xFFD4ECE8),
    onTertiaryContainer = Color(0xFF173B36),
    background = Color(0xFFF6F6F9),
    onBackground = Color(0xFF2B2C33),
    surface = Color(0xFFF6F6F9),
    onSurface = Color(0xFF2B2C33),
    surfaceVariant = Color(0xFFE6E6EC),
    onSurfaceVariant = Color(0xFF61636E),
    surfaceTint = Color(0xFF5858A3),
    inverseSurface = Color(0xFF34353C),
    inverseOnSurface = Color(0xFFEDEDF2),
    error = Color(0xFFB8453F),
    onError = Color(0xFFFFFBFA),
    errorContainer = Color(0xFFF7DEDC),
    onErrorContainer = Color(0xFF5C1814),
    outline = Color(0xFF9294A0),
    outlineVariant = Color(0xFFDCDCE3),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFBFD),
    surfaceDim = Color(0xFFDCDCE2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFEFF3),
    surfaceContainer = Color(0xFFEAEAEF),
    surfaceContainerHigh = Color(0xFFE4E4EA),
    surfaceContainerHighest = Color(0xFFDEDEE5),
)

internal val CreatorDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9D9DE0),
    onPrimary = Color(0xFF20204A),
    primaryContainer = Color(0xFF3B3B6C),
    onPrimaryContainer = Color(0xFFDCDCF8),
    inversePrimary = Color(0xFF5858A3),
    secondary = Color(0xFFB4B5CC),
    onSecondary = Color(0xFF26273A),
    secondaryContainer = Color(0xFF35364A),
    onSecondaryContainer = Color(0xFFD9DAEC),
    tertiary = Color(0xFF8CCBC1),
    onTertiary = Color(0xFF14332F),
    tertiaryContainer = Color(0xFF2A4A45),
    onTertiaryContainer = Color(0xFFCDEBE5),
    background = Color(0xFF1F2025),
    onBackground = Color(0xFFD6D7DE),
    surface = Color(0xFF1F2025),
    onSurface = Color(0xFFD6D7DE),
    surfaceVariant = Color(0xFF2E2F36),
    onSurfaceVariant = Color(0xFF9C9EA9),
    surfaceTint = Color(0xFF9D9DE0),
    inverseSurface = Color(0xFFD6D7DE),
    inverseOnSurface = Color(0xFF2A2B31),
    error = Color(0xFFE8928B),
    onError = Color(0xFF4A1512),
    errorContainer = Color(0xFF5A2622),
    onErrorContainer = Color(0xFFF6D6D3),
    outline = Color(0xFF686A76),
    outlineVariant = Color(0xFF34353C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3B42),
    surfaceDim = Color(0xFF1A1B1F),
    surfaceContainerLowest = Color(0xFF191A1E),
    surfaceContainerLow = Color(0xFF24252A),
    surfaceContainer = Color(0xFF28292F),
    surfaceContainerHigh = Color(0xFF2D2E34),
    surfaceContainerHighest = Color(0xFF34353C),
)
