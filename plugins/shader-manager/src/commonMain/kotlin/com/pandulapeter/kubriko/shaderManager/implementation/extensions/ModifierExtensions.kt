package com.pandulapeter.kubriko.shaderManager.implementation.extensions

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.shaderManager.Shader

internal fun Modifier.runtimeShader(
    shaderWrappers: List<Shader>,
) = shaderWrappers.fold(this) { compoundModifier, shaderWrapper ->
    compoundModifier then runtimeShader(shaderWrapper)
}

internal expect fun Modifier.runtimeShader(
    shaderWrapper: Shader,
): Modifier

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}