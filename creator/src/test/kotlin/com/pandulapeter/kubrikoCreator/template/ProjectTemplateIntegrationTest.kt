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
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenPosition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.gradle.CompilationMode
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFiles
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Generates every template for real, adds an Actor type and two Manager types that use every Event and every Block
 * (and each other's Properties), and compiles it all for every target. It downloads Gradle and every dependency, so it
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
                val buildFile = directory.resolve(KubrikoBuildFiles.BUILD_FILE)
                assertFalse("libs.kubriko.collision" in buildFile.readText(), "${template.id}: a plugin nothing uses")
                GradleProjectConnection(directory).use { connection ->
                    assertEquals(":", ProjectInspector.desktopModulePathOf(connection), template.id)
                    assertCompiles(connection, directory, "${template.id} as created")
                    BlockTypeStore.open(directory)?.let { store ->
                        directory.resolve("src/commonMain/composeResources/drawable/every-block.png").apply { parentFile.mkdirs() }.let { image ->
                            ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", image)
                        }
                        (listOf(everyBlockInEveryEvent, everyManager, hud) + family).forEach(store::save)
                        store.regenerateAll()
                        KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal).sync()
                        listOf("audioPlayback", "collision", "keyboardInput", "sprites").forEach { assertContains(buildFile.readText(), "implementation(libs.kubriko.$it)") }
                        assertCompiles(connection, directory, "${template.id} with every Block")
                        assertRuntimeClasspathResolves(connection, "${template.id} with every Block")
                    }
                }
            } finally {
                parentDirectory.deleteRecursively()
            }
        }
    }

    private fun assertCompiles(connection: GradleProjectConnection, directory: File, description: String) {
        listOf("desktop" to "compileKotlinDesktop", "wasmJs" to "compileKotlinWasmJs").forEach { (target, task) ->
            val output = StringBuilder()
            val isSuccessful = runBlocking {
                connection.run(taskPath = task, arguments = emptyList(), onEvent = { output.appendLine(it.toString()) })
            }
            assertTrue(isSuccessful, "$description doesn't compile for $target:\n$output")
            val classes = directory.resolve("build/classes/kotlin/$target/main")
            assertTrue(classes.walkTopDown().any { it.isFile }, "$description: nothing was compiled for $target")
        }
    }

    /**
     * Resolves the desktop runtime classpath the way Kubriko Creator does before playing, which compiling alone doesn't:
     * a library missing from the project's repositories only shows up here.
     */
    private fun assertRuntimeClasspathResolves(connection: GradleProjectConnection, description: String) {
        val classpathFile = File.createTempFile("kubriko-creator-classpath", ".txt")
        try {
            val output = StringBuilder()
            val isSuccessful = runBlocking {
                connection.run(
                    taskPath = ":${SessionInitScript.CLASSPATH_TASK_NAME}",
                    arguments = SessionInitScript.gradleArguments(
                        mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
                        CompilationMode(isCommonCodeIncremental = true, isClean = false),
                    ),
                    onEvent = { output.appendLine(it.toString()) },
                )
            }
            assertTrue(isSuccessful, "$description: the desktop runtime classpath doesn't resolve:\n$output")
        } finally {
            classpathFile.delete()
        }
    }

    private val properties = listOf(
        PropertyDefinition.Number("score", 2.5f),
        PropertyDefinition.Text("title", "Say \"\$hi\" \\ to\neveryone"),
        PropertyDefinition.Toggle("isShielded", true),
    )

    private val everyBlock = listOf(
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
        BlockDefinition.AddActor("EveryBlock", x = 10f, y = -5f),
        BlockDefinition.AddActor("Ball", isRelative = false),
        BlockDefinition.AddActor("Missing"),
        BlockDefinition.MoveTowardsNearest("Ball", speed = 120f),
        BlockDefinition.MoveTowardsNearest("EveryBlock"),
        BlockDefinition.MoveTowardsNearest(),
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
        BlockDefinition.If(ConditionDefinition.Paused(isPaused = false)),
        BlockDefinition.If(ConditionDefinition.Number("missing")),
        BlockDefinition.Stop,
        BlockDefinition.Else,
        BlockDefinition.Else,
        BlockDefinition.RemoveSelf,
        BlockDefinition.End,
        BlockDefinition.ChangeNumber(property = "score", by = 2f, manager = "EveryManager"),
        BlockDefinition.SetText(property = "title", value = "Hi", manager = "EveryManager"),
        BlockDefinition.SetToggle(property = "isShielded", manager = "EveryManager"),
        BlockDefinition.SetNumber(property = "score", manager = "Missing"),
        BlockDefinition.SetNumber(manager = "EveryManager"),
        BlockDefinition.If(ConditionDefinition.Number("score", Comparison.GREATER, 1f, manager = "EveryManager")),
        BlockDefinition.If(ConditionDefinition.Text("title", manager = "EveryManager")),
        BlockDefinition.If(ConditionDefinition.Toggle("isShielded", manager = "EveryManager")),
        BlockDefinition.Log(message = "{EveryManager.score} {EveryManager.title} {EveryManager.isShielded} {Missing.score} {EveryManager.missing}"),
        BlockDefinition.Kotlin(code = "actorManager.allActors.value.size"),
        BlockDefinition.GoToScene("main"),
        BlockDefinition.GoToScene("missing"),
        BlockDefinition.GoToScene(),
        BlockDefinition.RestartScene,
        BlockDefinition.PauseGame(PauseAction.PAUSE),
        BlockDefinition.PauseGame(PauseAction.RESUME),
        BlockDefinition.PauseGame(PauseAction.TOGGLE),
        BlockDefinition.PlaySound("files/sounds/hit.wav"),
        BlockDefinition.PlaySound("files/sounds/say \"\$hi\".wav"),
        BlockDefinition.PlaySound(),
        BlockDefinition.PlayMusic("files/music/theme.mp3"),
        BlockDefinition.PlayMusic("files/music/win.mp3", isLooping = false),
        BlockDefinition.PlayMusic(),
        BlockDefinition.StopMusic,
    )

    /**
     * Kotlin Blocks are inserted verbatim, so the ones about the Actor's velocity are left out of Manager types.
     */
    private val everyManagerBlock = everyBlock.filterNot { it is BlockDefinition.Kotlin && "velocity" in it.code }

    private fun everyEvent(blocks: List<BlockDefinition>) = EventType.entries.flatMap { type ->
        when (type) {
            EventType.KEY_PRESSED -> KeyChoice.entries.map { key -> EventDefinition(type = type, key = key, blocks = blocks) }
            EventType.COLLISION -> listOf("EveryBlock", "Ball").map { EventDefinition(type = type, otherActorType = it, blocks = blocks + blocks) }
            EventType.TIMER_FINISHED -> listOf(1, 8, 1).map { EventDefinition(type = type, timer = it, blocks = blocks) }
            else -> listOf(EventDefinition(type = type, blocks = blocks))
        }
    }

    private val everyBlockInEveryEvent = ActorTypeDefinition(
        name = "EveryBlock",
        shape = ActorShape.RECTANGLE,
        sprite = "drawable/every-block.png",
        properties = properties + PropertyDefinition.Number("everyManager"),
        events = everyEvent(everyBlock),
    )

    /**
     * Three generations of Actor types: every Event in each (so every hook is overridden, running the parent's Event
     * first), a new Property and a changed initial value in the child, collisions with the family by its parent, and
     * a sprite in the grandchild.
     */
    private val family = listOf(
        ActorTypeDefinition(
            name = "EveryParent",
            properties = properties,
            events = everyEvent(everyBlock),
        ),
        ActorTypeDefinition(
            name = "EveryChild",
            parent = "EveryParent",
            shape = ActorShape.RECTANGLE,
            properties = listOf(properties.first(), PropertyDefinition.Number("extra", initialValue = 2f)),
            events = everyEvent(listOf(BlockDefinition.RunParentEvent) + everyBlock + BlockDefinition.MoveTowardsNearest("EveryParent", speed = 10f)) +
                EventDefinition(EventType.COLLISION, otherActorType = "EveryParent", blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.RemoveOther)),
        ),
        ActorTypeDefinition(
            name = "EveryGrandchild",
            parent = "EveryChild",
            sprite = "drawable/every-block.png",
            properties = listOf(PropertyDefinition.Toggle("isLast")),
            events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.RunParentEvent, BlockDefinition.Stop, BlockDefinition.SetToggle("isLast", value = true)))),
        ),
    )

    /**
     * Uses its own Properties both by themselves and by its own name.
     */
    private val everyManager = ManagerTypeDefinition(
        name = "EveryManager",
        properties = properties,
        events = everyEvent(everyManagerBlock),
        texts = ScreenPosition.entries.map { ScreenTextDefinition("{score} {EveryManager.title} \"\$\" {missing}", it, size = 12.5f, color = 0x80FF0000) },
    )

    private val hud = ManagerTypeDefinition(
        name = "Hud",
        properties = properties,
        events = everyEvent(everyManagerBlock),
        texts = listOf(ScreenTextDefinition("{EveryManager.score} / {score}"), ScreenTextDefinition()),
        kotlinUi = """
            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.windowInsetsPadding
            import androidx.compose.foundation.text.BasicText
            import androidx.compose.runtime.collectAsState
            import androidx.compose.ui.Modifier

            Column(modifier = Modifier.windowInsetsPadding(windowInsets)) {
                if (!stateManager.isRunning.collectAsState().value && !isInSceneEditor) BasicText("Paused: ${'$'}title")
                BasicText("${'$'}{everyManager.score + score} ${'$'}{actorManager.allActors.collectAsState().value.size} ${'$'}{viewportManager.size.collectAsState().value}")
            }
        """.trimIndent(),
    )
}
