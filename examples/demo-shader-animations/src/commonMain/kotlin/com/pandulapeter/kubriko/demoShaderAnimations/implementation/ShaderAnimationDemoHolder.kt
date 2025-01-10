package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.ShaderManager

internal class ShaderAnimationDemoHolder<SHADER : Shader<STATE>, STATE : Shader.State>(
    shader: SHADER,
    updater: (SHADER, STATE) -> Unit,
    nameForLogging: String,
) {
    val manager = ShaderAnimationsDemoManager(shader, updater)
    val kubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        manager,
        isLoggingEnabled = true,
        instanceNameForLogging = "ShaderAnimation-$nameForLogging",
    )
}