package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.implementation.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader : Unique, Actor {
    val shaderCode: String
    val uniformName: String get() = "content"
    val uniformsBlock: (ShaderUniformProvider.() -> Unit)? get() = null
}