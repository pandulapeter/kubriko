package com.pandulapeter.kubriko.shader

import androidx.compose.ui.Modifier

/**
 * TODO: Documentation + mention the limitation in Android
 */
expect fun Modifier.runtimeShader(
    shader: String,
    uniformName: String = "content",
    uniformsBlock: (ShaderUniformProvider.() -> Unit)? = null,
): Modifier

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}