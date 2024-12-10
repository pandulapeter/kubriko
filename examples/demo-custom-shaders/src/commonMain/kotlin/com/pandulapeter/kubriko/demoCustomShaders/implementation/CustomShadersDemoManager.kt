package com.pandulapeter.kubriko.demoCustomShaders.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class CustomShadersDemoManager<SHADER : Shader<STATE>, STATE : Shader.State>(
    private val shader: SHADER,
    private val updater: (SHADER, STATE) -> Unit,
) : Manager() {

    private lateinit var metadataManager: MetadataManager
    private val _shaderState = MutableStateFlow(shader.state)
    val shaderState = _shaderState.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        metadataManager = kubriko.require()
        kubriko.require<ActorManager>().add(shader)
        _shaderState.onEach { updater(shader, it) }.launchIn(scope)
    }

    fun setState(state: STATE) = _shaderState.update { state }
}