/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.preloadFont
import org.jetbrains.compose.resources.preloadImageBitmap
import org.jetbrains.compose.resources.preloadImageVector

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun preloadedFont(
    resource: FontResource,
    weight: FontWeight,
    style: FontStyle
): State<Font?> = preloadFont(
    resource = resource,
    weight = weight,
    style = style,
)

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun preloadedImageBitmap(
    resource: DrawableResource,
): State<ImageBitmap?> = preloadImageBitmap(
    resource = resource
)

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun preloadedImageVector(
    resource: DrawableResource,
): State<ImageVector?> = preloadImageVector(
    resource = resource
)