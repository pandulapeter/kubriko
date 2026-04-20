/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
            val result = ArrayList<Shader<*>>()
            val seenStates = HashSet<Any?>()
            allActors.forEach { actor ->
                if (actor is Shader<*>) {
                    if (seenStates.add(actor.shaderState)) {
                        result.add(actor)
                    }
                }
            }
            result.toImmutableList()
        }.asStateFlow(persistentListOf())
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>): Modifier {
        if (!isInitialized.collectAsState().value) return modifier
        var currentModifier = modifier
        shaders.collectAsState().value.forEach { shader ->
            if (shader.layerIndex == layerIndex) {
                currentModifier = currentModifier.then(Modifier.shader(shader, gameTime))
            }
        }
        return currentModifier
    }
}