/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.benchmark

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A project the size of a big GameMaker game, for [CreatorBenchmarkTest]: [ACTOR_TYPE_COUNT] Actor types in families of
 * ten (each with Properties, and Events that refer to other types), [MANAGER_TYPE_COUNT] Manager types with texts, and
 * Scenes of every size in [SCENE_SIZES].
 */
internal object BenchmarkProject {

    const val ACTOR_TYPE_COUNT = 200
    const val MANAGER_TYPE_COUNT = 20
    val SCENE_SIZES = listOf(50, 1_000, 5_000)
    const val PACKAGE_NAME = "com.example.benchmark"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val offlineGenerator = ProjectGenerator(
        CreatorConfiguration(kubrikoVersion = "1.0.0", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = false),
        wrapperInstaller = { _, _ -> },
        startVersionControl = { _, _ -> },
    )

    fun actorTypeName(index: Int) = "Enemy$index"

    fun managerTypeName(index: Int) = "Manager$index"

    fun sceneName(size: Int) = "scene-$size"

    /**
     * Creates the project in [parentDirectory] with [generator] (by default one that installs no Gradle wrapper, so the
     * project can't be built), with its definitions and Scenes written as files (generating its code is part of what is
     * measured), and returns its folder.
     */
    fun create(parentDirectory: File, generator: ProjectGenerator = offlineGenerator): File {
        val directory = generator.create(NewProjectRequest(name = "Benchmark", packageName = PACKAGE_NAME, parentDirectory = parentDirectory, isUnderVersionControl = false))
        val actors = File(directory, "${BlockTypeStore.CREATOR_DIRECTORY}/actors").apply { deleteRecursively() }
        repeat(ACTOR_TYPE_COUNT) { index ->
            val folder = File(actors, "family${index / 10}").apply { mkdirs() }
            File(folder, "${actorTypeName(index)}.json").writeText(json.encodeToString(ActorTypeDefinition.serializer(), actorType(index)) + "\n")
        }
        val managers = File(directory, "${BlockTypeStore.CREATOR_DIRECTORY}/managers").apply { mkdirs() }
        repeat(MANAGER_TYPE_COUNT) { index ->
            File(managers, "${managerTypeName(index)}.json").writeText(json.encodeToString(ManagerTypeDefinition.serializer(), managerType(index)) + "\n")
        }
        val scenes = File(directory, SceneStore.DEFAULT_SCENE_DIRECTORY)
        SCENE_SIZES.forEach { size -> File(scenes, "${sceneName(size)}.json").writeText(SceneDocument.format(scene(size))) }
        return directory
    }

    private fun actorType(index: Int): ActorTypeDefinition {
        val other = actorTypeName((index + 1) % ACTOR_TYPE_COUNT)
        return ActorTypeDefinition(
            name = actorTypeName(index),
            parent = if (index % 10 == 0) null else actorTypeName(index - index % 10),
            shape = if (index % 2 == 0) ActorShape.CIRCLE else ActorShape.RECTANGLE,
            width = 32f,
            height = 32f,
            properties = listOf(
                PropertyDefinition.Number("hp", initialValue = 3f),
                PropertyDefinition.Text("label", "Enemy"),
                PropertyDefinition.Toggle("isAngry"),
            ),
            events = listOf(
                EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.SetVelocity(x = 40f, y = 20f), BlockDefinition.StartTimer(timer = 1, seconds = 2f, isRepeating = true))),
                EventDefinition(
                    EventType.EVERY_FRAME,
                    blocks = listOf(
                        BlockDefinition.If(ConditionDefinition.Number("hp", Comparison.LESS_OR_EQUAL, 0f)),
                        BlockDefinition.ChangeNumber("score", by = 10f, manager = managerTypeName(index % MANAGER_TYPE_COUNT)),
                        BlockDefinition.RemoveSelf,
                        BlockDefinition.End,
                    ),
                ),
                EventDefinition(EventType.COLLISION, otherActorType = other, blocks = listOf(BlockDefinition.BounceOffOther, BlockDefinition.ChangeNumber("hp", by = -1f), BlockDefinition.Log("{label} was hit: {hp}"))),
                EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE, blocks = listOf(BlockDefinition.SetToggle("isAngry", value = true), BlockDefinition.MoveTowardsNearest(other, speed = 80f))),
                EventDefinition(EventType.TIMER_FINISHED, timer = 1, blocks = listOf(BlockDefinition.AddActor(actorTypeName((index + 7) % ACTOR_TYPE_COUNT), x = 16f))),
                EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge)),
            ),
        )
    }

    private fun managerType(index: Int) = ManagerTypeDefinition(
        name = managerTypeName(index),
        properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Number("lives", initialValue = 3f)),
        events = listOf(
            EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.SetNumber("score", value = 0f))),
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.Number("lives", Comparison.LESS_OR_EQUAL, 0f)), BlockDefinition.RestartScene, BlockDefinition.End)),
        ),
        texts = listOf(ScreenTextDefinition("Score: {score}"), ScreenTextDefinition("Lives: {lives}")),
    )

    private fun scene(size: Int) = List(size) { index ->
        SceneActorEntry(
            typeId = actorTypeName(index % ACTOR_TYPE_COUNT),
            state = SceneDocument.blockBuiltState(x = (index % 100) * 40f, y = (index / 100) * 40f, width = 32f, height = 32f),
            id = "a$index",
        )
    }
}
