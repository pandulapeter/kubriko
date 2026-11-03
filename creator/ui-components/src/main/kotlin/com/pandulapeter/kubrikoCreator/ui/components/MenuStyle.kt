/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How every menu of Kubriko Creator looks: a brighter surface than any panel, with an edge and a shadow, so it reads as
 * something floating above the panel it opened from, whatever its width.
 */
public object MenuStyle {
    /**
     * The menu's background.
     */
    public val color: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceBright

    /**
     * The menu's edge.
     */
    public val border: BorderStroke
        @Composable
        @ReadOnlyComposable
        get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    /**
     * The menu's corners.
     */
    public val shape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    /**
     * The menu's shadow.
     */
    public val elevation: Dp = 12.dp
}
