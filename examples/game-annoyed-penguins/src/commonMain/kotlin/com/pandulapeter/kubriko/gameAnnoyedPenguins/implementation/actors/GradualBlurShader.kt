/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.shaders.collection.BlurShader

internal class GradualBlurShader : BlurShader(
    shaderState = State(
        blurHorizontal = 0f,
        blurVertical = 0f,
    )
), Dynamic {

    override fun update(deltaTimeInMilliseconds: Int) {
        if (shaderState.blurHorizontal < 20f) {
            shaderState = shaderState.copy(
                blurHorizontal = shaderState.blurHorizontal + 0.1f,
                blurVertical = shaderState.blurVertical + 0.1f,
            )
        }
    }
}