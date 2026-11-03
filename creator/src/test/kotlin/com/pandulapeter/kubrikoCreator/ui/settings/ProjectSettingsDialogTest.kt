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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginReason
import com.pandulapeter.kubrikoCreator.actors.codegen.PluginUse
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.AspectRatio
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.GameSettingsReading
import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import com.pandulapeter.kubrikoCreator.project.ProjectSettingsSnapshot
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProjectSettingsDialogTest {
    private val identity = GameIdentity("Breakout", "1.0.0", 3, "com.example.breakout", "Pandula", "")

    @Test
    fun typingAValidValueChangesItAndAnInvalidOneSaysWhy() = uiTest(width = 640, height = 900) {
        val changes = mutableListOf<SetProjectSettings.Input>()
        var shown by mutableStateOf(identity)
        showing {
            ProjectSettingsDialog(
                settings = ProjectSettingsSnapshot(shown, game = null, libraries = emptyList(), scenes = emptyList()),
                onChange = { changes += it; shown = it.appliedTo(shown) },
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        onNodeWithText("Breakout").assertExists()
        onNodeWithText("com.example.breakout").assertExists()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Breakout!")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("1.0.")
        waitForIdle()
        assertEquals(listOf(SetProjectSettings.Input(name = "Breakout!")), changes)
        onNodeWithText("isn't a version name", substring = true).assertExists()
        saveSnapshot("project-settings")
    }

    @Test
    fun aProjectWhoseBuildDoesNotReadItsSettingsOffersTheUpgrade() = uiTest(width = 640, height = 600) {
        var isReviewing = false
        showing {
            ProjectSettingsDialog(
                settings = ProjectSettingsSnapshot(identity = null, game = null, libraries = emptyList(), scenes = emptyList()),
                onChange = {},
                onReviewUpgrade = { isReviewing = true },
                onDismiss = {},
            )
        }
        onNodeWithText("Upgrade its template files first", substring = true).assertExists()
        onNodeWithText("Review changes").performClick()
        assertEquals(true, isReviewing)
        saveSnapshot("project-settings-unavailable")
    }

    @Test
    fun settingsThatCanNotBeReadSayWhyAndOfferNoUpgrade() = uiTest(width = 640, height = 600) {
        showing {
            ProjectSettingsDialog(
                settings = null,
                failure = IOException("gradle.properties (Permission denied)"),
                onChange = {},
                onReviewUpgrade = {},
                onDismiss = {},
            )
        }
        onNodeWithText("Couldn't read this project's settings: gradle.properties (Permission denied)").assertExists()
        onNodeWithText("Review changes").assertDoesNotExist()
        saveSnapshot("project-settings-unreadable")
    }

    @Test
    fun theGameSectionChangesTheGamesSettingsAndListsTheLibrariesWithWhy() = uiTest(width = 640, height = 1400) {
        val changes = mutableListOf<SetProjectSettings.Input>()
        var game by mutableStateOf(GameSettings(aspectRatio = AspectRatio.FIT_VERTICAL))
        val libraries = listOf(
            LibraryInUse(KubrikoArtifact.ENGINE),
            LibraryInUse(KubrikoArtifact.COLLISION, uses = listOf(PluginUse(KubrikoArtifact.COLLISION, PluginReason.COLLISION_EVENT, "Ball", "Paddle")), isUsedByGeneratedCode = true),
            LibraryInUse(KubrikoArtifact.PHYSICS, handWrittenFiles = listOf("src/commonMain/kotlin/com/example/breakout/Gravity.kt")),
            LibraryInUse(KubrikoArtifact.SERIALIZATION, isUsedByGeneratedCode = true),
        )
        showing {
            ProjectSettingsDialog(
                settings = ProjectSettingsSnapshot(identity, GameSettingsReading(game, listOf(GameSettings.FRAME_RATE)), libraries, scenes = listOf("main", "levels/1-1")),
                onChange = { changes += it; game = it.appliedTo(game) },
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        onNodeWithText("Fit the height").assertExists()
        onNodeWithText("60 updates per second").assertExists()
        onNodeWithText("game.frameRate", substring = true).assertExists()
        onNodeWithText("Ball collides with Paddle").assertExists()
        onNodeWithText("Used in src/commonMain/kotlin/com/example/breakout/Gravity.kt").assertExists()
        onNodeWithText("Used by the code Kubriko Creator generates.").assertExists()
        onNodeWithText("Every game uses it.").assertExists()
        saveSnapshot("project-settings-game")
        onNodeWithText("Used by the code Kubriko Creator generates.").performScrollTo()
        saveSnapshot("project-settings-libraries")
        onNodeWithText("Height").performTextReplacement("0")
        waitForIdle()
        onNodeWithText("The viewport's width and height are numbers from 1 to 100000.").assertExists()
        onNodeWithText("Height").performTextReplacement("720")
        waitForIdle()
        onNodeWithText("Fit the height").performClick()
        onNodeWithText("Fixed").performClick()
        waitForIdle()
        onNodeWithText("Width").assertExists()
        assertEquals(listOf(SetProjectSettings.Input(viewportHeight = 720f), SetProjectSettings.Input(aspectRatio = AspectRatio.FIXED)), changes)
    }

    @Test
    fun aFirstSceneThatIsGoneSaysSo() = uiTest(width = 640, height = 900) {
        showing {
            ProjectSettingsDialog(
                settings = ProjectSettingsSnapshot(identity, GameSettingsReading(GameSettings(firstScene = "deleted")), emptyList(), scenes = listOf("main")),
                onChange = {},
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        onNodeWithText("There's no Scene called deleted.").assertExists()
    }

    @Test
    fun anIdentityWithoutGameSettingsOffersTheUpgradeForThem() = uiTest(width = 640, height = 900) {
        showing {
            ProjectSettingsDialog(
                settings = ProjectSettingsSnapshot(identity, game = null, libraries = emptyList(), scenes = emptyList()),
                onChange = {},
                onReviewUpgrade = {},
                onDismiss = {},
            )
        }
        onNodeWithText("doesn't read its first Scene, aspect ratio, and frame rate", substring = true).assertExists()
        onNodeWithText("Review changes").assertExists()
    }

    @Test
    fun theFrameRateMenuOffersTheCommonRatesAndTheCurrentOne() {
        assertEquals(listOf(30, 60, 120, GameSettings.EVERY_DISPLAY_FRAME), frameRateOptions(60))
        assertEquals(listOf(30, 60, 75, 120, GameSettings.EVERY_DISPLAY_FRAME), frameRateOptions(75))
        assertEquals(listOf(30, 60, 120, GameSettings.EVERY_DISPLAY_FRAME), frameRateOptions(GameSettings.EVERY_DISPLAY_FRAME))
    }

    @Test
    fun aLibraryUsedByItsTypesIsntAlsoSaidToBeUsedByTheGeneratedCode() {
        val use = PluginUse(KubrikoArtifact.SPRITES, PluginReason.SPRITE, "Ball", "drawable/ball.png")
        assertEquals(
            listOf(LibraryReason.Use(use), LibraryReason.File("src/Game.kt")),
            reasonsOf(LibraryInUse(KubrikoArtifact.SPRITES, listOf(use), listOf("src/Game.kt"), isUsedByGeneratedCode = true)),
        )
        assertEquals(listOf(LibraryReason.Generated), reasonsOf(LibraryInUse(KubrikoArtifact.SCENE_EDITOR_API, isUsedByGeneratedCode = true)))
        assertEquals(listOf(LibraryReason.Always), reasonsOf(LibraryInUse(KubrikoArtifact.ENGINE, isUsedByGeneratedCode = true)))
    }

    @Test
    fun aVersionNumberThatIsNotANumberIsInvalid() {
        val change = IdentityField.VERSION_NUMBER.changeOf(identity, "four")
        assertEquals(IdentityProblem.VERSION_NUMBER_INVALID, (change as IdentityFieldChange.Invalid).reason)
        assertEquals(IdentityFieldChange.Valid(SetProjectSettings.Input(versionNumber = 4)), IdentityField.VERSION_NUMBER.changeOf(identity, " 4 "))
        assertEquals("3", IdentityField.VERSION_NUMBER.valueOf(identity))
    }
}
