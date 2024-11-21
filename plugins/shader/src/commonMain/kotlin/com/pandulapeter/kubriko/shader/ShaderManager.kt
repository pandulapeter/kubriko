package com.pandulapeter.kubriko.shader

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shader.implementation.ShaderManagerImpl

/**
 * TODO: Documentation
 */
abstract class ShaderManager : Manager() {

    companion object {
        fun newInstance(): ShaderManager = ShaderManagerImpl()

        const val CONTENT = "content"
        const val RESOLUTION = "resolution"
    }
}