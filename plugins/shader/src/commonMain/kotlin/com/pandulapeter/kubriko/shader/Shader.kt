package com.pandulapeter.kubriko.shader

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.CanvasAware
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.StateFlow

//TODO: Documentation
interface Shader<T : Shader.State> : CanvasAware, Actor {

    val state: StateFlow<T>
    val code: String

    interface State {
        // TODO: Delegates could be used to simplify this
        fun ShaderUniformProvider.applyUniforms() = Unit
    }
}