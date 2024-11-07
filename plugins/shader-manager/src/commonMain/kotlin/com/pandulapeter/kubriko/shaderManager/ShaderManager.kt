package com.pandulapeter.kubriko.shaderManager

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.shaderManager.implementation.ShaderManagerImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
abstract class ShaderManager : Manager() {

    abstract val allShaders: StateFlow<List<Shader>>

    abstract fun add(vararg shaders: Shader)

    abstract fun remove(vararg shaders: Shader)

    abstract fun removeAll()

    companion object {
        fun newInstance(vararg initialShader: Shader): ShaderManager = ShaderManagerImpl(initialShader = initialShader)
    }
}