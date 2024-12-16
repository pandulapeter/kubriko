package com.pandulapeter.kubriko.demoShaderAnimations.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager

internal class ShaderAnimationDemoHolder<SHADER : Shader<STATE>, STATE : Shader.State>(
    shader: SHADER,
    updater: (SHADER, STATE) -> Unit,
) {
    val manager = ShaderAnimationsDemoManager(shader, updater)
    val kubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        manager,
    )
}