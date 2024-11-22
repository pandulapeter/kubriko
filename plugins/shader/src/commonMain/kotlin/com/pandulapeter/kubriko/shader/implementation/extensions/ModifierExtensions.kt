package com.pandulapeter.kubriko.shader.implementation.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shader.Shader

internal fun <T : Shader.State> Modifier.runtimeShader(
    shader: Shader<T>,
) = this then graphicsLayer {
    clip = true
    renderEffect = shader(shader, size)
}

internal expect fun <T : Shader.State> shader(
    shader: Shader<T>,
    size: Size,
): RenderEffect?

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}