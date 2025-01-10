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