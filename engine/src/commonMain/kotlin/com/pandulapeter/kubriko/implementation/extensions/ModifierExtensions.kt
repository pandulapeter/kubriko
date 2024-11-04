package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.actor.traits.Shader

internal fun Modifier.runtimeShader(
    shaderWrappers: List<Shader>,
): Modifier = this.let { modifier ->
    var temp = modifier
    shaderWrappers.forEach { wrapper ->
        temp = temp.runtimeShader(wrapper)
    }
    temp
}

internal expect fun Modifier.runtimeShader(
    shaderWrapper: Shader,
): Modifier

interface ShaderUniformProvider {
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value1: Float, value2: Float)
}