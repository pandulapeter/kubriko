package com.pandulapeter.kubriko.shaderManager.implementation.extensions

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shaderManager.Shader

internal actual fun shader(shader: Shader, size: Size): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val runtimeShader = RuntimeShader(shader.code)
        val shaderUniformProvider = ShaderUniformProviderImpl(runtimeShader)
        return RenderEffect.createRuntimeShaderEffect(
            runtimeShader.apply {
                shader.applyUniforms(shaderUniformProvider)
                shaderUniformProvider.updateResolution(size)
            },
            shader.uniformName,
        ).asComposeRenderEffect()
    } else {
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ShaderUniformProviderImpl(
    private val runtimeShader: RuntimeShader,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform("resolution", size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShader.setIntUniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShader.setFloatUniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShader.setFloatUniform(name, value1, value2)
}