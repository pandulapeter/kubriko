/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.ui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp

/**
 * A ripple [androidx.compose.foundation.Indication] with a fixed [color] and [rippleAlpha].
 *
 * Material 3 no longer exposes the ripple alpha through `RippleConfiguration`, so the games build
 * their own ripple to keep the stronger, more playful alphas their themes are designed around.
 * Themes provide this through `LocalIndication`, which only reaches components that take their
 * indication from it — [GameButton] rather than Material's own `FloatingActionButton`.
 */
@Stable
fun gameRipple(
    color: Color,
    rippleAlpha: RippleAlpha,
): IndicationNodeFactory = GameRipple(
    color = color,
    rippleAlpha = rippleAlpha,
)

@Stable
private class GameRipple(
    private val color: Color,
    private val rippleAlpha: RippleAlpha,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode = createRippleModifierNode(
        interactionSource = interactionSource,
        bounded = true,
        radius = Dp.Unspecified,
        color = { color },
        rippleAlpha = { rippleAlpha },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameRipple) return false
        return color == other.color && rippleAlpha == other.rippleAlpha
    }

    override fun hashCode(): Int = 31 * color.hashCode() + rippleAlpha.hashCode()
}
