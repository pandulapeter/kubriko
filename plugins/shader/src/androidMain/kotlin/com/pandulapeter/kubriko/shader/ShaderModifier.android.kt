package com.pandulapeter.kubriko.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

actual fun Modifier.runtimeShader(
    shader: String,
    uniformName: String,
    uniformsBlock: (ShaderUniformProvider.() -> Unit)?,
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) this then composed {
    val runtimeShader = remember { RuntimeShader(shader) }
    val shaderUniformProvider = remember { ShaderUniformProviderImpl(runtimeShader) }
    graphicsLayer {
        clip = true
        renderEffect = RenderEffect
            .createRuntimeShaderEffect(
                runtimeShader.apply {
                    uniformsBlock?.invoke(shaderUniformProvider)
                    shaderUniformProvider.updateResolution(size)
                },
                uniformName,
            ).asComposeRenderEffect()
    }
} else this

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class ShaderUniformProviderImpl(
    private val runtimeShader: RuntimeShader,
) : ShaderUniformProvider {

    fun updateResolution(size: Size) = uniform("resolution", size.width, size.height)

    override fun uniform(name: String, value: Int) = runtimeShader.setIntUniform(name, value)

    override fun uniform(name: String, value: Float) = runtimeShader.setFloatUniform(name, value)

    override fun uniform(name: String, value1: Float, value2: Float) = runtimeShader.setFloatUniform(name, value1, value2)
}