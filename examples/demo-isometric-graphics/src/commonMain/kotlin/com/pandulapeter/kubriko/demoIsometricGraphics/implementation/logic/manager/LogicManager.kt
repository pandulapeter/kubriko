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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    override fun onInitialize(kubriko: Kubriko) {
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
            val mainCharacter = MainCharacter(
                cuboidModel = characterModel,
                position = Vec3(
                    x = 5000.sceneUnit,
                    y = 6000.sceneUnit,
                    z = SceneUnit.Zero,
                ),
                textureResolver = textureResolver::resolveTexture,
            )
            actorManager.add(
                mainCharacter,
                Character(
                    mainCharacter = mainCharacter,
                    cuboidModel = characterModel,
                    position = Vec3(
                        x = 5000.sceneUnit,
                        y = 5500.sceneUnit,
                        z = SceneUnit.Zero,
                    ),
                    textureResolver = textureResolver::resolveTexture,
                ),
                Character(
                    mainCharacter = mainCharacter,
                    cuboidModel = characterModel,
                    position = Vec3(
                        x = 3500.sceneUnit,
                        y = 5000.sceneUnit,
                        z = SceneUnit.Zero,
                    ),
                    textureResolver = textureResolver::resolveTexture,
                ),
            )
            val characterPositions = listOf(
                mainCharacter.renderableCuboidModel.position.positionXY,
                Vec3(5000.sceneUnit, 5500.sceneUnit, SceneUnit.Zero).positionXY,
                Vec3(3500.sceneUnit, 5000.sceneUnit, SceneUnit.Zero).positionXY,
            )
            val treePositions = mutableListOf<SceneOffset>()
            val minDistance = 400.sceneUnit
            var attempts = 0
            while ((treePositions.size < 128) && (attempts < 10000)) {
                val newPosition = SceneOffset(
                    x = Random.nextInt(0, 12500).sceneUnit,
                    y = Random.nextInt(0, 12500).sceneUnit
                )
                val isTooCloseToCharacter = characterPositions.any { it.distanceTo(newPosition) < minDistance }
                val isTooCloseToTree = treePositions.any { it.distanceTo(newPosition) < minDistance }

                if (!isTooCloseToCharacter && !isTooCloseToTree) {
                    treePositions.add(newPosition)
                }
                attempts++
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
        }
    } catch (_: MissingResourceException) {
    } catch (_: SerializationException) {
    }
}