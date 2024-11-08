package com.pandulapeter.kubriko.shaderManager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.CanvasAware
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader : CanvasAware, Unique, Actor {

    val code: String

    fun applyUniforms(provider: ShaderUniformProvider) = Unit
}