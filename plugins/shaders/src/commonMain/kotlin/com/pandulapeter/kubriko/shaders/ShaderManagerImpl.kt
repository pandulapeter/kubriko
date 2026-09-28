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
import com.pandulapeter.kubriko.actor.Actor
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
            allActors.distinctShaders().toImmutableList()
        }.asStateFlowOnMainThread(persistentListOf())
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

/**
 * The shaders in this list, skipping true duplicates: a shader of the same class, on the same layer and with an equal
 * state as one kept earlier would only apply the same effect twice.
 */
internal fun List<Actor>.distinctShaders(): List<Shader<*>> {
    val result = ArrayList<Shader<*>>()
    forEach { actor ->
        if (actor is Shader<*> && result.none { it.isDuplicateOf(actor) }) {
            result.add(actor)
        }
    }
    return result
}

private fun Shader<*>.isDuplicateOf(other: Shader<*>) = this::class == other::class && layerIndex == other.layerIndex && shaderState == other.shaderState
