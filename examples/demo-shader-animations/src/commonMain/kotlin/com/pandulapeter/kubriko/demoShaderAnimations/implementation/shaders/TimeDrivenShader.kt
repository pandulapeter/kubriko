/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader

/**
 * A [Shader] whose `time` uniform follows the instance's active runtime, so the animation pauses with the game.
 */
internal abstract class TimeDrivenShader<S : Shader.State>(
    initialState: S,
) : Shader<S>, Dynamic {
    final override var shaderState = initialState
        private set
    private var time = 0f
    private lateinit var metadataManager: MetadataManager

    protected abstract fun S.withTime(time: Float): S

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f
        shaderState = shaderState.withTime(time)
    }

    fun updateState(state: S) {
        shaderState = state.withTime(time)
    }
}
