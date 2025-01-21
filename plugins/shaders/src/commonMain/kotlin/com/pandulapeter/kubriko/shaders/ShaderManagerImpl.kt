/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.shaders.extensions.shader
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

internal class ShaderManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : ShaderManager(isLoggingEnabled, instanceNameForLogging) {
    override val areShadersSupported = com.pandulapeter.kubriko.shaders.extensions.areShadersSupported
    private val actorManager by manager<ActorManager>()
    private val shaders by autoInitializingLazy {
        actorManager.allActors.map { allActors ->
            allActors
                .filterIsInstance<Shader<*>>()
                .distinctBy { it.shaderState }
                .toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = if (isInitialized.collectAsState().value) {
        shaders.collectAsState().value
            .filter { it.layerIndex == layerIndex }
            .fold(modifier) { compoundModifier, shader ->
                compoundModifier then Modifier.shader(shader)
            }
    } else {
        modifier
    }
}