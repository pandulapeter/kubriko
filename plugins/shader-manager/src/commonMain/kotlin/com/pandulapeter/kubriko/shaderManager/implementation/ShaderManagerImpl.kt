package com.pandulapeter.kubriko.shaderManager.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.ShaderManager
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.runtimeShader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class ShaderManagerImpl : ShaderManager() {

    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
    }

    @Composable
    override fun onCreateModifier() = Modifier.runtimeShader(
        actorManager.allActors
            .map { actors -> actors.filterIsInstance<Shader>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
            .collectAsState()
            .value
    )
}