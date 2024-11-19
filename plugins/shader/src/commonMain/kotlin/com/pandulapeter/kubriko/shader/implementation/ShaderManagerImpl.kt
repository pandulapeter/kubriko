package com.pandulapeter.kubriko.shader.implementation

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.runtimeShader
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl : ShaderManager() {

    private lateinit var actorManager: ActorManager

    override fun getModifier(canvasIndex: Int?) = actorManager.allActors
        .map { it.filterIsInstance<Shader>().filter { it.canvasIndex == canvasIndex } }
        .map { shaders ->
            shaders.fold<Shader, Modifier>(Modifier) { compoundModifier, shader ->
                compoundModifier then Modifier.runtimeShader(shader)
            }
        }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
    }
}