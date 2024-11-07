package com.pandulapeter.kubriko.shaderManager.implementation.extensions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun shader(shader: Shader, size: Size): RenderEffect? {
    val runtimeShaderBuilder = RuntimeShaderBuilder(
        effect = RuntimeEffect.makeForShader(shader.code),
    )
    val shaderUniformProvider = ShaderUniformProviderImpl(runtimeShaderBuilder)
    return ImageFilter.makeRuntimeShader(
        runtimeShaderBuilder = runtimeShaderBuilder.apply {
            shader.applyUniforms(shaderUniformProvider)
            shaderUniformProvider.updateResolution(size)
        },
        shaderName = ShaderManager.UNIFORM_CONTENT,
        input = null,
    ).asComposeRenderEffect()
}

private class ShaderUniformProviderImpl(
    private val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(ShaderManager.UNIFORM_RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShaderBuilder.uniform(name, value1, value2)
}