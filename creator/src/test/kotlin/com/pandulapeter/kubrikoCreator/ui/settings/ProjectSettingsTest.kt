/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.HistoryEntry
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.commands.project.SetTarget
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetHost
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testAndroidVersions
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalTestApi::class)
class ProjectSettingsTest {
    private val root = testDirectory()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun theSettingsAreReadFromTheProjectAndTheLibrariesFollowTheTypes() = uiTest(width = 640, height = 1600) {
        File(root, "build.gradle.kts").writeText("// Reads game.name and game.firstScene.\n")
        File(root, "gradle.properties").writeText(GameSettings(firstScene = "levels/1-1").writtenTo(GameIdentity("Breakout", "1.0.0", 1, "com.example.breakout", "", "").writtenTo("")))
        File(root, "src/commonMain/kotlin/Gravity.kt").apply { parentFile.mkdirs() }.writeText("import com.pandulapeter.kubriko.physics.RigidBody\n")
        File(root, "src/commonMain/kotlin/generated/Ball.kt").apply { parentFile.mkdirs() }.writeText("import com.pandulapeter.kubriko.collision.Collidable\n")
        val types = MutableStateFlow<List<BlockTypeDefinition>>(emptyList())
        showing {
            ProjectSettings(
                rootDirectory = root,
                blockTypes = types,
                scenes = SceneStore(root) { emptyList() },
                targets = null,
                history = MutableStateFlow(CommandHistory()),
                onCommand = {},
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Used in src/commonMain/kotlin/Gravity.kt")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Breakout").assertExists()
        onNodeWithText("levels/1-1").assertExists()
        types.value = listOf(ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Ball"))))
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Ball collides with Ball")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun anUndoWhileItIsOpenShowsTheSettingsItBroughtBack() = uiTest(width = 640, height = 1600) {
        File(root, "build.gradle.kts").writeText("// Reads game.name.\n")
        val properties = File(root, "gradle.properties").apply { writeText(identityNamed("Breakout")) }
        val renaming = HistoryEntry(revert = Invocation(SetProjectSettings, SetProjectSettings.Input(name = "Pong")), reapply = Invocation(SetProjectSettings, SetProjectSettings.Input(name = "Breakout")))
        val history = MutableStateFlow(CommandHistory(undoable = listOf(renaming)))
        showing {
            ProjectSettings(
                rootDirectory = root,
                blockTypes = null,
                scenes = SceneStore(root) { emptyList() },
                targets = null,
                history = history,
                onCommand = {},
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Breakout")).fetchSemanticsNodes().isNotEmpty() }
        properties.writeText(identityNamed("Pong"))
        history.value = CommandHistory(redoable = listOf(renaming))
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Pong")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Breakout").assertDoesNotExist()
    }

    @Test
    fun aTargetIsTurnedOnOnlyOnceItsChangeIsConfirmed() = uiTest(width = 900, height = 1600) {
        val project = ProjectGenerator(testConfiguration(), wrapperInstaller = { _, _ -> }, startVersionControl = { _, _ -> })
            .create(NewProjectRequest(name = "Breakout", packageName = "com.example.breakout", parentDirectory = root))
        val sdk = File(root, "sdk").apply { File(this, "platforms").mkdirs() }
        val targets = GameTargets(
            project,
            testAndroidVersions,
            object : TargetHost {
                override fun androidSdk(rootDirectory: File) = sdk

                override val canBuildIos = false
            },
        )
        val commands = mutableListOf<Invocation<*>>()
        showing {
            ProjectSettings(
                rootDirectory = project,
                blockTypes = null,
                scenes = SceneStore(project) { emptyList() },
                targets = targets,
                history = MutableStateFlow(CommandHistory()),
                onCommand = { commands += it },
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Android")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("iOS needs a Mac with Xcode, which this computer doesn't have.").performScrollTo()
        saveSnapshot("project-settings-targets")
        onNodeWithText("Android").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Build the game for Android")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("+ include(\":androidApp\")").assertExists()
        onNodeWithText("New files: ", substring = true).assertExists()
        saveSnapshot("project-settings-target-change")
        onNodeWithText("Apply changes").performClick()
        waitForIdle()
        val shown = assertIs<TargetChange.Proposed>(targets.change(GameTarget.ANDROID, isOn = true))
        assertEquals(listOf<Invocation<*>>(Invocation(SetTarget, SetTarget.Input(GameTarget.ANDROID, isOn = true, changeId = shown.id))), commands)
        onNodeWithText("Build the game for Android").assertDoesNotExist()
        assertFalse(File(project, "androidApp").exists())
    }

    private fun identityNamed(name: String) = GameIdentity(name, "1.0.0", 1, "com.example.game", "", "").writtenTo("")
}
