package com.pandulapeter.kubriko.shaderManager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader : Unique, Actor {
    val shaderCode: String
    val uniformName: String get() = "content"
    val uniformsBlock: (ShaderUniformProvider.() -> Unit)? get() = null
}