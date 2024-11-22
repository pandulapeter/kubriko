package com.pandulapeter.kubriko.shader.implementation.extensions

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun <T : Shader.State> shader(
    shader: Shader<T>,
    size: Size,
): RenderEffect? {
    val runtimeShaderBuilder = RuntimeShaderBuilder(
        effect = RuntimeEffect.makeForShader(shader.code),
    )
    val shaderUniformProvider = ShaderUniformProviderImpl(runtimeShaderBuilder)
    return ImageFilter.makeRuntimeShader(
        runtimeShaderBuilder = runtimeShaderBuilder.apply {
            with(shader.state.value) { shaderUniformProvider.applyUniforms() }
            shaderUniformProvider.updateResolution(size)
        },
        shaderName = ShaderManager.CONTENT,
        input = null,
    ).asComposeRenderEffect()
}

private class ShaderUniformProviderImpl(
    private val runtimeShaderBuilder: RuntimeShaderBuilder,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform(ShaderManager.RESOLUTION, size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShaderBuilder.uniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShaderBuilder.uniform(name, value1, value2)
}