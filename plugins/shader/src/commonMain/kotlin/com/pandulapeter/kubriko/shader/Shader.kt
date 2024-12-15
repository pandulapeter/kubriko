package com.pandulapeter.kubriko.shader

import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader<T : Shader.State> : LayerAware {

    val state: T
    val cache: Cache
    val code: String

    interface State {
        // TODO: Delegates could be used to simplify this
        fun ShaderUniformProvider.applyUniforms() = Unit
    }

    class Cache {
        internal var runtimeShader: Any? = null
        internal var uniformProvider: ShaderUniformProvider? = null
    }

    companion object {
        const val RESOLUTION = "resolution"
    }
}