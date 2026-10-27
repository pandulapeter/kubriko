/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeStore
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Generates every template for real, adds an Actor type that uses every Event and every Block, and compiles it all for
 * every target. It downloads Gradle and every dependency, so it
 * only runs when the `KUBRIKO_CREATOR_INTEGRATION` environment variable is `true`. Against unreleased modules, also set
 * `KUBRIKO_CREATOR_VERSION` to the version published to Maven Local (see creator/CLAUDE.md).
 */
class ProjectTemplateIntegrationTest {

    @Test
    fun everyTemplateGeneratesAProjectThatCompilesForEveryTarget() {
        if (System.getenv("KUBRIKO_CREATOR_INTEGRATION") != "true") return
        val bundled = CreatorConfiguration.load()
        val configuration = System.getenv("KUBRIKO_CREATOR_VERSION")?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
        ProjectTemplate.entries.forEach { template ->
            val parentDirectory = createTempDirectory().toFile()
            try {
                val directory = ProjectGenerator(configuration).create(
                    NewProjectRequest(name = "Integration Game", packageName = "com.example.integration", parentDirectory = parentDirectory, template = template),
                )
                assertTrue(directory.resolve("gradlew").canExecute(), "${template.id}: gradlew")
                ActorTypeStore.open(directory)?.save(everyBlockInEveryEvent)
                GradleProjectConnection(directory).use { connection ->
                    assertEquals(":", ProjectInspector.inspect(connection).desktopModulePath, template.id)
                    listOf("desktop" to "compileKotlinDesktop", "wasmJs" to "compileKotlinWasmJs").forEach { (target, task) ->
                        val output = StringBuilder()
                        val isSuccessful = runBlocking {
                            connection.run(taskPath = task, arguments = emptyList(), onEvent = { output.appendLine(it.toString()) })
                        }
                        assertTrue(isSuccessful, "${template.id} doesn't compile for $target:\n$output")
                        val classes = directory.resolve("build/classes/kotlin/$target/main")
                        assertTrue(classes.walkTopDown().any { it.isFile }, "${template.id}: nothing was compiled for $target")
                    }
                }
            } finally {
                parentDirectory.deleteRecursively()
            }
        }
    }

    private val everyBlockInEveryEvent = ActorTypeDefinition(
        name = "EveryBlock",
        shape = ActorShape.RECTANGLE,
        properties = listOf(
            PropertyDefinition.Number("score", 2.5f),
            PropertyDefinition.Text("title", "Say \"\$hi\" \\ to\neveryone"),
            PropertyDefinition.Toggle("isShielded", true),
        ),
        events = EventType.entries.flatMap { type ->
            val blocks = listOf(
                BlockDefinition.SetVelocity(x = 1.5f, y = -2f),
                BlockDefinition.AddToVelocity(x = 1e-4f, y = 3f),
                BlockDefinition.Stop,
                BlockDefinition.JumpTo(x = -10f, y = 10f),
                BlockDefinition.BounceOffEdge,
                BlockDefinition.RemoveSelf,
                BlockDefinition.BounceOffOther,
                BlockDefinition.RemoveOther,
                BlockDefinition.Log(message = "Say \"\$hi\" \\ to\neveryone"),
                BlockDefinition.Kotlin(code = "val speed = velocity.x\nif (speed > 0f.sceneUnit) velocity = SceneOffset.Zero"),
                BlockDefinition.SetNumber(property = "score", value = -1f),
                BlockDefinition.ChangeNumber(property = "score", by = 0.5f),
                BlockDefinition.SetText(property = "title", value = "Game \"over\""),
                BlockDefinition.SetToggle(property = "isShielded", value = false),
                BlockDefinition.SetNumber(),
                BlockDefinition.ChangeNumber(property = "missing"),
                BlockDefinition.Log(message = "{title}: {score} ({isShielded}) {missing}"),
                BlockDefinition.Kotlin(code = "if (isShielded) score += 1f"),
                BlockDefinition.StartTimer(timer = 1, seconds = 0.25f),
                BlockDefinition.StartTimer(timer = 8, seconds = 2f, isRepeating = true),
                BlockDefinition.StopTimer(timer = 2),
                BlockDefinition.StartTimer(timer = 9),
                BlockDefinition.End,
                BlockDefinition.If(ConditionDefinition.Number("score", Comparison.LESS_OR_EQUAL, 3f)),
                BlockDefinition.Repeat(times = 2),
                BlockDefinition.If(ConditionDefinition.Text("title", "Say \"\$hi\"", isEqual = false)),
                BlockDefinition.If(ConditionDefinition.Toggle("isShielded")),
                BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.SPACE, isHeld = false)),
                BlockDefinition.If(ConditionDefinition.Velocity(Axis.VERTICAL, Comparison.NOT_EQUAL, -1.5f)),
                BlockDefinition.If(ConditionDefinition.Chance(12.5f)),
                BlockDefinition.If(ConditionDefinition.Number("missing")),
                BlockDefinition.Stop,
                BlockDefinition.Else,
                BlockDefinition.Else,
                BlockDefinition.RemoveSelf,
                BlockDefinition.End,
            )
            when (type) {
                EventType.KEY_PRESSED -> KeyChoice.entries.map { key -> EventDefinition(type = type, key = key, blocks = blocks) }
                EventType.COLLISION -> listOf("EveryBlock", "Ball").map { EventDefinition(type = type, otherActorType = it, blocks = blocks + blocks) }
                EventType.TIMER_FINISHED -> listOf(1, 8, 1).map { EventDefinition(type = type, timer = it, blocks = blocks) }
                else -> listOf(EventDefinition(type = type, blocks = blocks))
            }
        },
    )
}
