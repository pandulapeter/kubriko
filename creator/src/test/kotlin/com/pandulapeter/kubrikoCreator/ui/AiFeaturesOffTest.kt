/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.DescriptionPreview
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.mcp.AiActivityBar
import com.pandulapeter.kubrikoCreator.ui.settings.CreatorSettingsDialog
import com.pandulapeter.kubrikoCreator.ui.theme.ThemeChoice
import com.pandulapeter.kubrikoCreator.workspace.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.creator.generated.resources.Res
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * With AI features off (Creator settings), the parts of the app that show AI show none of it.
 */
@OptIn(ExperimentalTestApi::class)
class AiFeaturesOffTest {

    @Composable
    private fun WorkspaceParts() = Column {
        WorkspaceToolbar(
            sessionState = SessionState.Idle,
            history = MutableStateFlow(CommandHistory()),
            onUndo = {},
            onRedo = {},
            gitState = MutableStateFlow(GitState.NoRepository),
            isAiConnected = true,
            openPanel = SidePanel.AI,
            onPanelToggled = {},
            playTargets = PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP),
            onPlayTargetsOpened = {},
            onPlayTargetChosen = {},
            onPlay = {},
            onStop = {},
            onOpenProjectSettings = {},
            onOpenTargets = {},
            onSetUpMoreDevices = {},
            onOpenSettings = {},
            onCloseProject = {},
        )
        AiActivityBar(
            steps = listOf(AiStep(id = 0, toolName = "edit_block", target = null, isRunning = true, clientName = "Claude Code")),
            isFollowing = true,
            onFollowingChange = {},
            onClick = {},
        )
        CompositionLocalProvider(LocalDescriptionWriter provides DescriptionWriter(isBusy = false) {}) {
            DescriptionPreview(name = "Ball", description = null, maximumLength = 200, onDescriptionChange = {}, action = { WriteDescriptionButton(prompt = "") })
        }
        WelcomeScreen(error = null, errorArguments = emptyList(), recentProjects = emptyList(), onNewProject = {}, onOpenProject = {}, onForgetRecentProject = {}, onOpenSettings = {})
        WorkspaceHint(Modifier.height(240.dp))
    }

    private fun ComposeUiTest.shownTexts() = onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                node.config.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
        }

    @Test
    fun theWorkspaceShowsNoAiWithAiFeaturesOff() = uiTest(width = 1280, height = 900) {
        showing { CompositionLocalProvider(LocalIsAiEnabled provides false) { WorkspaceParts() } }
        val aiTexts = shownTexts().filter { AI_WORDS.containsMatchIn(it) }
        assertEquals(emptyList(), aiTexts)
        onNodeWithText("Settings").assertExists()
        saveSnapshot("ai-off-workspace")
    }

    @Test
    fun theSamePartsShowTheAiWithAiFeaturesOn() = uiTest(width = 1280, height = 900) {
        showing { WorkspaceParts() }
        assertTrue(shownTexts().any { AI_WORDS.containsMatchIn(it) })
        onNodeWithText("AI").assertExists()
    }

    @Test
    fun theSettingsTurnAiFeaturesOffAndChooseTheThemeSizeAndHowTheAiShows() = uiTest(width = 960, height = 640) {
        val changes = mutableListOf<Any?>()
        showing {
            CreatorSettingsDialog(
                settings = CreatorSettings(),
                homeFolder = File("/Users/me"),
                onAiEnabledChange = { changes += it },
                onThemeChange = { changes += it },
                onInterfaceScaleChange = { changes += it },
                onDefaultProjectFolderChange = { changes += it },
                onFollowingAiChange = { changes += "follow $it" },
                onAiFollowDelayChange = { changes += it },
                onTurnReviewShownChange = { changes += "review $it" },
                onDismiss = {},
            )
        }
        onNodeWithText("Dark").performClick()
        onNodeWithText("100%").performClick()
        saveSnapshot("creator-settings")
        onNodeWithText("Editor").performClick()
        onNodeWithText(File("/Users/me").path).assertExists()
        onNodeWithText("Use the home folder").assertDoesNotExist()
        onNodeWithText("AI").performClick()
        onNodeWithText("Follow the AI").performClick()
        onNodeWithText("1 s").performClick()
        onNodeWithText("Keep and Undo after each answer").performClick()
        onNodeWithText("AI features").performClick()
        assertEquals(listOf<Any?>(ThemeChoice.DARK, 1f, "follow false", 1_000, "review true", false), changes)
        saveSnapshot("creator-settings-ai")
    }

    private companion object {
        val AI_WORDS = Regex("""\b(AI|Claude|assistant|agent|MCP)\b""", RegexOption.IGNORE_CASE)
    }
}
