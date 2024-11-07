package com.pandulapeter.kubriko.shaderManager

import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader {

    val code: String

    fun applyUniforms(provider: ShaderUniformProvider) = Unit
}