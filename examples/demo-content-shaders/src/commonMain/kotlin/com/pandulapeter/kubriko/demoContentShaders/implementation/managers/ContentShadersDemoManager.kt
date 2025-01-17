/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoContentShaders.implementation.actors.ColorfulBox
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.extensions.times
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.RippleShader
import com.pandulapeter.kubriko.shaders.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset

internal class ContentShadersDemoManager : Manager() {

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(
            (-10..10).flatMap { y ->
                (-10..10).map { x ->
                    ColorfulBox(
                        initialPosition = SceneOffset(
                            x = x * 100.sceneUnit,
                            y = y * 100.sceneUnit,
                        ),
                        hue = (0..360).random().toFloat(),
                    )
                }
            }
                    + SmoothPixelationShader()
                    + VignetteShader()
                    + RippleShader()
                    + ChromaticAberrationShader()
        )
    }
}