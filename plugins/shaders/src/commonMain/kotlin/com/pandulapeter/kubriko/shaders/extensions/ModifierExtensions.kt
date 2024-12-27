package com.pandulapeter.kubriko.shaders.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shaders.Shader

fun <T : Shader.State> Modifier.shader(
    shader: Shader<T>,
) = this then graphicsLayer {
    clip = true
    renderEffect = createRenderEffect(shader, size)
}

internal expect fun <T : Shader.State> createRenderEffect(
    shader: Shader<T>,
    size: Size,
): RenderEffect?

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}