package com.pandulapeter.kubriko.shaders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shaders.extensions.shader
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl(
    isLoggingEnabled: Boolean
) : ShaderManager(isLoggingEnabled) {
    override val areShadersSupported = com.pandulapeter.kubriko.shaders.extensions.areShadersSupported
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
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = if (isInitialized.value) {
        shaders.value
            .filter { it.layerIndex == layerIndex }
            .fold(modifier) { compoundModifier, shader ->
                compoundModifier then Modifier.shader(shader)
            }
    } else {
        modifier
    }
}