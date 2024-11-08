package com.pandulapeter.kubriko.shader.implementation.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.pandulapeter.kubriko.shader.Shader

internal fun Modifier.runtimeShader(
    shader: Shader,
) = this then graphicsLayer {
    clip = true
    renderEffect = shader(shader, size)
}

internal expect fun shader(shader: Shader, size: Size): RenderEffect?

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}