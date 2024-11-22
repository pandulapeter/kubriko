package com.pandulapeter.kubriko.shader.implementation

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.runtimeShader
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl : ShaderManager() {

    private lateinit var actorManager: ActorManager
    private val shaders by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<Shader<*>>()
                .distinctBy { it.state }
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    override fun getModifier(canvasIndex: Int?) = shaders.value
        .fold<Shader<*>, Modifier>(Modifier) { compoundModifier, shader ->
            compoundModifier then Modifier.runtimeShader(shader)
        }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
    }
}