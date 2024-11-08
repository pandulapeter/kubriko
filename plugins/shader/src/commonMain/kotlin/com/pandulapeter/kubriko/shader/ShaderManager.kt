package com.pandulapeter.kubriko.shader

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shader.implementation.ShaderManagerImpl

/**
 * TODO: Documentation
 */
abstract class ShaderManager : Manager() {

    companion object {
        fun newInstance(vararg initialShaders: Shader): ShaderManager = ShaderManagerImpl(initialShaders = initialShaders)

        const val UNIFORM_CONTENT = "content"
        const val UNIFORM_RESOLUTION = "resolution"
    }
}