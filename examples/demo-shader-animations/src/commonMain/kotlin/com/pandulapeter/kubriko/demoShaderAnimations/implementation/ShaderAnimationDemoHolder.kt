/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.managers.ShaderAnimationsDemoManager
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.TimeDrivenShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.ShaderManager

internal class ShaderAnimationDemoHolder<STATE : Shader.State>(
    shader: TimeDrivenShader<STATE>,
    nameForLogging: String,
    isLoggingEnabled: Boolean,
    stateControls: @Composable (state: STATE, onStateChanged: (STATE) -> Unit) -> Unit,
) {
    val shaderManager = ShaderManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = "$LOG_TAG-$nameForLogging",
    )
    val shaderAnimationsDemoManager = ShaderAnimationsDemoManager(shader)
    val kubriko = Kubriko.newInstance(
        shaderManager,
        shaderAnimationsDemoManager,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = "$LOG_TAG-$nameForLogging",
    )
    val code = shader.shaderCode
    val controls: @Composable () -> Unit = {
        stateControls(shaderAnimationsDemoManager.shaderState.collectAsState().value, shaderAnimationsDemoManager::setState)
    }
}

private const val LOG_TAG = "ShaderAnimation"