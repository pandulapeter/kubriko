package com.pandulapeter.kubriko.shader.implementation.extensions

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager

internal actual fun <T : Shader.State> shader(
    shader: Shader<T>,
    size: Size,
): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val runtimeShader = (shader.cache.runtimeShader as? RuntimeShader) ?: RuntimeShader(shader.code).also { shader.cache.runtimeShader = it}
        val shaderUniformProvider = (shader.cache.uniformProvider as? ShaderUniformProviderImpl) ?: ShaderUniformProviderImpl(runtimeShader).also { shader.cache.uniformProvider = it }
        return RenderEffect.createRuntimeShaderEffect(
            runtimeShader.apply {
                with(shader.state) { shaderUniformProvider.applyUniforms() }
                shaderUniformProvider.updateResolution(size)
            },
            ShaderManager.CONTENT,
        ).asComposeRenderEffect()
    } else {
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ShaderUniformProviderImpl(
    private val runtimeShader: RuntimeShader,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(ShaderManager.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShader.setIntUniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShader.setFloatUniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShader.setFloatUniform(name, value1, value2)
}