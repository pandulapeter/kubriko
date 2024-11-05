package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.implementation.manager.ShaderManagerImpl

/**
 * TODO: Documentation
 */
// TODO: Move to plugin
abstract class ShaderManager : Manager() {

    companion object {
        fun newInstance(): ShaderManager = ShaderManagerImpl()
    }
}