package com.pandulapeter.kubriko.shaderManager

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaderManager.implementation.ShaderManagerImpl

/**
 * TODO: Documentation
 */
abstract class ShaderManager : Manager() {

    companion object {
        fun newInstance(): ShaderManager = ShaderManagerImpl()
    }
}