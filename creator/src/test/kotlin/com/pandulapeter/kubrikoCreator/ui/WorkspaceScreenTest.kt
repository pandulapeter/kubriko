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

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.rightClick
import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.Assistant
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSettingsStore
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantStorage
import com.pandulapeter.kubrikoCreator.ai.assistant.FakeAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.SecretStore
import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.ProcessRunner
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.workspace.workspaceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import kotlin.test.Test

/**
 * The whole workspace screen on the Breakout fixture, with no Gradle, git, or AI tool behind it, at the sizes a window can
 * have: everything side by side in a roomy one, and folded into tabs rather than crushed in the smallest.
 */
@OptIn(ExperimentalTestApi::class)
class WorkspaceScreenTest {

    private object NoProcesses : ProcessRunner {
        override fun exists(command: String) = false
        override fun locate(command: String): String? = null
        override fun run(command: String, arguments: List<String>) = ProcessRunner.Result(127, "")
        override fun aliases() = emptyMap<String, String>()
    }

    private object NoSecrets : SecretStore {
        override val isAvailable = false
        override fun read(name: String): String? = null
        override fun write(name: String, value: String) = false
        override fun delete(name: String) = Unit
    }

    private fun showingTheBall(width: Int, height: Int, isAiPanelOpen: Boolean = true, block: ComposeUiTest.() -> Unit) = uiTest(width, height) {
        val directory = testDirectory()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val controller = workspaceController(
            project = CreatorProject(rootDirectory = directory, name = "Breakout"),
            parentScope = scope,
            ioDispatcher = Dispatchers.IO,
            blockTypeRepository = FakeBlockTypeRepository(BreakoutFixture.types),
            openGitRepository = { null },
        )
        val mcp = McpController(
            server = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
            store = McpSettingsStore(File(directory, "mcp.json")),
            scope = scope,
            ioDispatcher = Dispatchers.IO,
        )
        val assistant = AssistantController(
            scope = scope,
            runner = NoProcesses,
            storage = AssistantStorage(settings = AssistantSettingsStore(File(directory, "assistant.json")), secrets = NoSecrets),
            signInLauncher = FakeAgentLauncher(),
            ioDispatcher = Dispatchers.IO,
            createAssistant = { command, _, profile ->
                Assistant(
                    agent = AcpAgent(command, FakeAgentLauncher(), AcpAgent.clientInfo("1.0.0"), scope, Dispatchers.IO),
                    toolServer = McpToolServer(executor = { null }, callDispatcher = Dispatchers.Default, version = "1.0.0"),
                    ioDispatcher = Dispatchers.IO,
                    profile = profile,
                )
            },
        )
        try {
            showing {
                WorkspaceScreen(
                    controller = controller,
                    mcp = mcp,
                    aiClients = AiClientsController(mcp = mcp, runner = NoProcesses, scope = scope, ioDispatcher = Dispatchers.IO),
                    assistant = assistant,
                    panelLayout = PanelLayout(emptyMap(), { _, _ -> }, {}),
                    onCloseProject = {},
                    onOpenSettings = {},
                )
            }
            awaitText("Ball")
            onNodeWithText("Ball").performClick()
            awaitText("When added to the Scene")
            if (isAiPanelOpen) {
                onNodeWithText("AI").performClick()
                awaitText("Assistant")
            }
            block()
        } finally {
            assistant.close()
            mcp.close()
            controller.close()
            scope.cancel()
        }
    }

    private fun ComposeUiTest.awaitText(text: String, substring: Boolean = false) =
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    private fun tab(title: String) = hasText(title) and isSelectable()

    @Test
    fun aRoomyWindowShowsEveryColumnSideBySideAndTheAiPanelOpensOnTheAssistant() = showingTheBall(1600, 1000) {
        onNodeWithText("Appearance").assertExists()
        onNode(tab("Details")).assertDoesNotExist()
        onNodeWithText("Your own AI tool").assertDoesNotExist()
        saveSnapshot("workspace-wide")
    }

    @Test
    fun theSmallestWindowFoldsTheDetailsIntoATabBesideTheEvents() = showingTheBall(960, 600, isAiPanelOpen = false) {
        onNodeWithText("Appearance").assertDoesNotExist()
        onNode(tab("Blocks")).assertDoesNotExist()
        saveSnapshot("workspace-narrow")
        onNode(tab("Details")).performClick()
        awaitText("Appearance")
    }

    @Test
    fun withASidePanelOpenTheSmallestWindowFoldsThePaletteTooSoTheBlocksKeepTheirRoom() = showingTheBall(960, 600) {
        onNode(tab("Blocks")).performClick()
        awaitText("Movement")
        saveSnapshot("workspace-narrowest")
    }

    @Test
    fun findingUsagesFromTheSidebarListsWhatRefersToATypeAndGoesThere() = showingTheBall(1600, 1000, isAiPanelOpen = false) {
        onAllNodesWithText("Brick").onFirst().performMouseInput { rightClick() }
        onNodeWithText("Find usages").performClick()
        awaitText("Usages of Brick")
        awaitText("Based on Brick")
        onNodeWithText("Collides with Brick").assertExists()
        saveSnapshot("workspace-usages")
        onNodeWithText("Based on Brick").performClick()
        awaitText("a brick worth more")
    }

    @Test
    fun searchingTheTextsFindsAScreenTextAndGoesToItsManagerType() = showingTheBall(1600, 1000, isAiPanelOpen = false) {
        onNodeWithText("Search").performClick()
        awaitText("Type to find text in the project", substring = true)
        onNode(hasSetTextAction() and hasText("Find in Blocks, texts, Kotlin, and pages")).performTextInput("lives:")
        awaitText("Lives: {lives}")
        onNodeWithText("Lives: {lives}").performClick()
        awaitText("the score and the lives")
    }
}
