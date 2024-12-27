package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class ShaderManager : Manager() {

    abstract val areShadersSupported: Boolean

    companion object {
        fun newInstance(): ShaderManager = ShaderManagerImpl()
    }
}