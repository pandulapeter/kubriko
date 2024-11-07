package com.pandulapeter.kubriko.shaderManager.implementation

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.runtimeShader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class ShaderManagerImpl(
    vararg initialShader: Shader,
) : ShaderManager() {

    private val _allShaders = MutableStateFlow(initialShader.toSet().distinctBy { it::class })
    override val allShaders = _allShaders.asStateFlow()
    override val modifier = _allShaders.map { shaders ->
        shaders.fold<Shader, Modifier>(Modifier) { compoundModifier, shader ->
            compoundModifier then Modifier.runtimeShader(shader)
        }
    }

    override fun add(vararg shaders: Shader) = _allShaders.update { currentShaders ->
        val uniqueNewShaderTypes = shaders.map { it::class }.toSet()
        val filteredCurrentShaders = currentShaders.filterNot { it::class in uniqueNewShaderTypes }
        filteredCurrentShaders + shaders
    }

    override fun remove(vararg shaders: Shader) = _allShaders.update { currentShaders ->
        currentShaders.filterNot { it in shaders }
    }

    override fun removeAll() = _allShaders.update { emptyList() }
}