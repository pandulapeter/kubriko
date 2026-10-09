/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.TimeDrivenShader
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaders.Shader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class ShaderAnimationsDemoManager<STATE : Shader.State>(
    private val shader: TimeDrivenShader<STATE>,
) : Manager() {
    private val _shaderState = MutableStateFlow(shader.shaderState)
    val shaderState = _shaderState.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(shader)
        _shaderState.onEach { shader.updateState(it) }.launchIn(scope)
    }

    fun setState(state: STATE) = _shaderState.update { state }
}