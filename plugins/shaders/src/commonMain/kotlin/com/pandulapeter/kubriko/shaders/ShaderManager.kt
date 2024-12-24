package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaders.implementation.ShaderManagerImpl

/**
 * TODO: Documentation
 */
abstract class ShaderManager : Manager() {

    abstract val areShadersSupported: Boolean

    companion object {
        fun newInstance(): ShaderManager = ShaderManagerImpl()
    }
}