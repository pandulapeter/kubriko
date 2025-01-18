/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.managers.ShaderAnimationsDemoManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.ShaderManager

internal class ShaderAnimationDemoHolder<SHADER : Shader<STATE>, STATE : Shader.State>(
    shader: SHADER,
    updater: (SHADER, STATE) -> Unit,
    nameForLogging: String,
) {
    private val shaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = "$LOG_TAG-$nameForLogging",
    )
    val shaderAnimationsDemoManager = ShaderAnimationsDemoManager(shader, updater)
    val kubriko = Kubriko.newInstance(
        shaderManager,
        shaderAnimationsDemoManager,
        isLoggingEnabled = true,
        instanceNameForLogging = "$LOG_TAG-$nameForLogging",
    )
}

private const val LOG_TAG = "ShaderAnimation"