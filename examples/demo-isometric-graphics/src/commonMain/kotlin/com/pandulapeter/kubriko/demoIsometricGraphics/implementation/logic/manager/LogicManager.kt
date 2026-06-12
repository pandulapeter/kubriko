/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.gameplay.resources.FileResolver
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.gameplay.resources.TextureResolver
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.actor.Bush
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.actor.Character
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.actor.MainCharacter
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.actor.Tree
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.helpers.extensions.distanceTo
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.MissingResourceException
import kotlin.random.Random

class LogicManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val textureResolver by manager<TextureResolver>()
    private val json = Json { ignoreUnknownKeys = true }
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.allActors
            .filter { it.isNotEmpty() }
            .take(1)
            .onEach {
                delay(300)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
        scope.launch {
            loadModelsAndSetActors()
        }
    }

    private suspend fun loadModelsAndSetActors() = try {
        json.run {
            val characterModel = decodeFromString<CuboidModel>(FileResolver.resolveFileAsString("model/character.json"))
            val treeModel = decodeFromString<CuboidModel>(FileResolver.resolveFileAsString("model/tree.json"))
            val bushModel = decodeFromString<CuboidModel>(FileResolver.resolveFileAsString("model/bush.json"))
            val mainCharacterPosition = Vec3(x = 5000.sceneUnit, y = 6000.sceneUnit, z = SceneUnit.Zero)
            val mainCharacter = MainCharacter(
                cuboidModel = characterModel,
                position = mainCharacterPosition,
                textureResolver = textureResolver::resolveTexture,
            )
            val npcMinDistance = 400.sceneUnit
            val npcPositions = mutableListOf<SceneOffset>()
            var npcAttempts = 0
            while (npcPositions.size < 64 && npcAttempts < 10000) {
                val candidate = SceneOffset(
                    x = Random.nextInt(0, 12500).sceneUnit,
                    y = Random.nextInt(0, 12500).sceneUnit,
                )
                val tooClose = mainCharacterPosition.positionXY.distanceTo(candidate) < npcMinDistance
                    || npcPositions.any { it.distanceTo(candidate) < npcMinDistance }
                if (!tooClose) npcPositions.add(candidate)
                npcAttempts++
            }
            val allCharacterPositions = buildList {
                add(mainCharacterPosition.positionXY)
                addAll(npcPositions)
            }
            actorManager.add(
                mainCharacter,
                *npcPositions.map { position ->
                    Character(
                        mainCharacter = mainCharacter,
                        cuboidModel = characterModel,
                        position = Vec3(position.x, position.y, SceneUnit.Zero),
                        textureResolver = textureResolver::resolveTexture,
                    )
                }.toTypedArray(),
            )
            val treeMinDistance = 400.sceneUnit
            val treePositions = mutableListOf<SceneOffset>()
            var treeAttempts = 0
            while (treePositions.size < 256 && treeAttempts < 10000) {
                val candidate = SceneOffset(
                    x = Random.nextInt(0, 12500).sceneUnit,
                    y = Random.nextInt(0, 12500).sceneUnit,
                )
                val tooClose = allCharacterPositions.any { it.distanceTo(candidate) < treeMinDistance }
                    || treePositions.any { it.distanceTo(candidate) < treeMinDistance }
                if (!tooClose) treePositions.add(candidate)
                treeAttempts++
            }
            treePositions.forEach { position ->
                actorManager.add(
                    Tree(
                        cuboidModel = treeModel,
                        position = Vec3(position.x, position.y, SceneUnit.Zero),
                        textureResolver = textureResolver::resolveTexture,
                    )
                )
            }
            val bushMinDistanceToCharacter = 400.sceneUnit
            val bushMinDistanceToTree = 150.sceneUnit
            val bushMinDistanceToBush = 150.sceneUnit
            val bushPositions = mutableListOf<SceneOffset>()
            var bushAttempts = 0
            while (bushPositions.size < 256 && bushAttempts < 10000) {
                val candidate = SceneOffset(
                    x = Random.nextInt(0, 12500).sceneUnit,
                    y = Random.nextInt(0, 12500).sceneUnit,
                )
                val tooClose = allCharacterPositions.any { it.distanceTo(candidate) < bushMinDistanceToCharacter }
                    || treePositions.any { it.distanceTo(candidate) < bushMinDistanceToTree }
                    || bushPositions.any { it.distanceTo(candidate) < bushMinDistanceToBush }
                if (!tooClose) bushPositions.add(candidate)
                bushAttempts++
            }
            bushPositions.forEach { position ->
                actorManager.add(
                    Bush(
                        cuboidModel = bushModel,
                        position = Vec3(position.x, position.y, SceneUnit.Zero),
                        textureResolver = textureResolver::resolveTexture,
                    )
                )
            }
        }
    } catch (_: MissingResourceException) {
    } catch (_: SerializationException) {
    }
}
