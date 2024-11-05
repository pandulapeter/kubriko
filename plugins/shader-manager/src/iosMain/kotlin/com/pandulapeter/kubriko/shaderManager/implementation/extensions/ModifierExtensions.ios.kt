package com.pandulapeter.kubriko.shaderManager.implementation.extensions

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shaderManager.Shader
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun Modifier.runtimeShader(
    shaderWrapper: Shader
) = this then composed {
    val runtimeShaderBuilder = remember {
        RuntimeShaderBuilder(
            effect = RuntimeEffect.makeForShader(shaderWrapper.shaderCode),
        )
    }
    val shaderUniformProvider = remember { ShaderUniformProviderImpl(runtimeShaderBuilder) }
    graphicsLayer {
        clip = true
        renderEffect = ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = runtimeShaderBuilder.apply {
                shaderWrapper.uniformsBlock?.invoke(shaderUniformProvider)
                shaderUniformProvider.updateResolution(size)
            },
            shaderName = shaderWrapper.uniformName,
            input = null,
        ).asComposeRenderEffect()
    }
}

private class ShaderUniformProviderImpl(
    private val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform("resolution", size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShaderBuilder.uniform(name, value1, value2)
}