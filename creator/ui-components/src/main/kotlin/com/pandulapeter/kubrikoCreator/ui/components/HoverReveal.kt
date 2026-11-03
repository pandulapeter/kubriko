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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * Whether a row is pointed at or holds the focus, for actions that only show then ([revealedWhen]), so a list of rows
 * doesn't repeat the same buttons on every line. The actions stay in place while hidden, so nothing moves when they show,
 * and a pointer that reaches one has always shown it first.
 */
@Stable
public class HoverRevealState internal constructor(
    internal val interactions: MutableInteractionSource,
) {
    internal var hasFocus by mutableStateOf(false)

    /**
     * Whether the row is pointed at or holds the focus.
     */
    public val isActive: Boolean
        @Composable get() = interactions.collectIsHoveredAsState().value || hasFocus
}

/**
 * Remembers a [HoverRevealState].
 */
@Composable
public fun rememberHoverRevealState(): HoverRevealState = remember { HoverRevealState(MutableInteractionSource()) }

/**
 * Lets [state] follow whether the pointer is over this row and whether anything in it is focused.
 */
public fun Modifier.hoverReveal(state: HoverRevealState): Modifier = hoverable(state.interactions).onFocusChanged { state.hasFocus = it.hasFocus }

/**
 * Fades this element in while [isShown], and out otherwise.
 */
@Composable
public fun Modifier.revealedWhen(isShown: Boolean): Modifier {
    val alpha by animateFloatAsState(if (isShown) 1f else 0f, CreatorMotion.short())
    return graphicsLayer { this.alpha = alpha }
}
