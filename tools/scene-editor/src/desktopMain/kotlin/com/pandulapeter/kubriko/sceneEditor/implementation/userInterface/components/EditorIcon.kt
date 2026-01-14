/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun EditorIcon(
    modifier: Modifier = Modifier,
    drawableResource: DrawableResource,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
) = Icon(
    modifier = modifier
        .size(24.dp)
        .clip(CircleShape)
        .alpha(if (isEnabled) 1f else 0.2f)
        .run { onClick?.let { clickable(enabled = isEnabled, onClick = onClick) } ?: this }
        .padding(4.dp),
    painter = painterResource(drawableResource),
    contentDescription = contentDescription,
)