package com.pandulapeter.kubriko.shaderManager.implementation

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.runtimeShader
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl(
    private val initialShaders: Array<out Shader>,
) : ShaderManager() {

    private lateinit var actorManager: ActorManager
    override val modifier by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<Shader>() }
            .map { shaders ->
                shaders.fold<Shader, Modifier>(Modifier) { compoundModifier, shader ->
                    compoundModifier then Modifier.runtimeShader(shader)
                }
            }
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        actorManager.add(actors = initialShaders)
    }
}