package com.pandulapeter.kubriko.shader.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.runtimeShader
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl : ShaderManager() {

    override val areShadersSupported = com.pandulapeter.kubriko.shader.implementation.extensions.areShadersSupported
    private val actorManager by manager<ActorManager>()
    private val shaders by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<Shader<*>>()
                .distinctBy { it.state }
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    @Composable
    override fun getModifier(layerIndex: Int?) = if (isInitialized) shaders.value
        .filter { it.layerIndex == layerIndex }
        .fold<Shader<*>, Modifier>(Modifier) { compoundModifier, shader ->
            compoundModifier then Modifier.runtimeShader(shader)
        } else Modifier
}