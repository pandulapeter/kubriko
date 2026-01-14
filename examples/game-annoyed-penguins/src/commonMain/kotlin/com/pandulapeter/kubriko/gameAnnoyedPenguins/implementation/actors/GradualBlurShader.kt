/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.shaders.collection.BlurShader
import kotlin.math.min

internal class GradualBlurShader : BlurShader(
    shaderState = State(
        blurHorizontal = 0f,
        blurVertical = 0f,
    )
), Dynamic {

    override fun update(deltaTimeInMilliseconds: Int) {
        if (shaderState.blurHorizontal <= MAXIMUM_BLUR_AMOUNT) {
            shaderState = shaderState.copy(
                blurHorizontal = min(MAXIMUM_BLUR_AMOUNT, shaderState.blurHorizontal + 0.05f * deltaTimeInMilliseconds),
                blurVertical = min(MAXIMUM_BLUR_AMOUNT, shaderState.blurVertical + 0.05f * deltaTimeInMilliseconds),
            )
        }
    }

    override fun onRemoved() {
        shaderState = State(
            blurHorizontal = 0f,
            blurVertical = 0f,
        )
    }

    companion object {
        private const val MAXIMUM_BLUR_AMOUNT = 10f
    }
}