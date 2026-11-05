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

import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.EditDocument
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettingsPage
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorkspaceUiStateTest {
    private val uiState = WorkspaceUiState()

    private fun step(id: Int) = AiStep(id = id, toolName = "add_block", target = CommandTarget.BlockType("Ball"), isRunning = false)

    @Test
    fun togglingAPanelOpensItAndTogglingAgainClosesIt() {
        uiState.togglePanel(SidePanel.AI)
        assertEquals(SidePanel.AI, uiState.openPanel)
        uiState.togglePanel(SidePanel.AI)
        assertNull(uiState.openPanel)
    }

    @Test
    fun openingAnotherPanelReplacesTheOpenOne() {
        uiState.togglePanel(SidePanel.AI)
        uiState.togglePanel(SidePanel.VERSION_CONTROL)
        assertEquals(SidePanel.VERSION_CONTROL, uiState.openPanel)
    }

    @Test
    fun onlyOpeningVersionControlAsksForAGitRefresh() {
        assertTrue(uiState.togglePanel(SidePanel.VERSION_CONTROL))
        assertFalse(uiState.togglePanel(SidePanel.VERSION_CONTROL))
        assertFalse(uiState.togglePanel(SidePanel.AI))
    }

    @Test
    fun theActivityBarOpensTheAiPanelEvenWhenAnotherIsOpen() {
        uiState.togglePanel(SidePanel.VERSION_CONTROL)
        uiState.showAiPanel()
        assertEquals(SidePanel.AI, uiState.openPanel)
        uiState.showAiPanel()
        assertEquals(SidePanel.AI, uiState.openPanel)
    }

    @Test
    fun showingTheAssistantOpensTheAiPanelOnItsOwnView() {
        uiState.togglePanel(SidePanel.VERSION_CONTROL)
        uiState.selectAiPanelView(AiPanelView.GAME_MAP)
        uiState.showAssistant()
        assertEquals(SidePanel.AI, uiState.openPanel)
        assertEquals(AiPanelView.ASSISTANT, uiState.aiPanelView)
    }

    @Test
    fun projectSettingsOpenOnThePageAskedForOrTheFirst() {
        uiState.showProjectSettings(ProjectSettingsPage.TARGETS)
        assertTrue(uiState.isShowingProjectSettings)
        assertEquals(ProjectSettingsPage.TARGETS, uiState.projectSettingsPage)
        uiState.hideProjectSettings()
        uiState.showProjectSettings()
        assertNull(uiState.projectSettingsPage)
    }

    @Test
    fun reviewingTheUpgradeFromProjectSettingsClosesTheSettings() {
        uiState.showProjectSettings()
        uiState.reviewUpgrade()
        assertFalse(uiState.isShowingProjectSettings)
        assertTrue(uiState.isReviewingUpgrade)
        uiState.finishReviewingUpgrade()
        assertFalse(uiState.isReviewingUpgrade)
    }

    @Test
    fun dismissingTheUpgradeNoticeIsRemembered() {
        assertFalse(uiState.isUpgradeNoticeDismissed)
        uiState.dismissUpgradeNotice()
        assertTrue(uiState.isUpgradeNoticeDismissed)
    }

    @Test
    fun revisitingAStepRemembersTheLatestStepThen() {
        val earlier = step(1)
        val latest = step(2)
        uiState.revisit(earlier, latestStepId = latest.id)
        assertSame(earlier, shownAiStep(latest, uiState.revisitedAiStep)?.step)
        val newer = step(3)
        assertSame(newer, shownAiStep(newer, uiState.revisitedAiStep)?.step)
    }

    @Test
    fun theAiPanelOpensOnTheAssistant() {
        assertEquals(AiPanelView.ASSISTANT, uiState.aiPanelView)
        uiState.selectAiPanelView(AiPanelView.GAME_MAP)
        assertEquals(AiPanelView.GAME_MAP, uiState.aiPanelView)
    }

    @Test
    fun aDeleteWaitsForItsConfirmationWhileOtherChangesAreDispatchedAtOnce() {
        val dispatched = mutableListOf<Invocation<*>>()
        val rename = Invocation(RenameActorType, RenameActorType.Input("Ball", "Puck"))
        val delete = Invocation(DeleteActorType, DeleteActorType.Input("Ball"))
        uiState.dispatch(rename, onSuccess = {}) { invocation, _ -> dispatched += invocation }
        assertEquals(listOf<Invocation<*>>(rename), dispatched)
        assertNull(uiState.pendingDelete)
        var isDone = false
        uiState.dispatch(delete, onSuccess = { isDone = true }) { invocation, _ -> dispatched += invocation }
        assertEquals(listOf<Invocation<*>>(rename), dispatched)
        assertSame(delete, uiState.pendingDelete?.invocation)
        uiState.pendingDelete?.onSuccess?.invoke()
        assertTrue(isDone)
        uiState.finishDeleting()
        assertNull(uiState.pendingDelete)
    }

    @Test
    fun deletingADocumentOrAddingItToTheScreenplayWaitsForItsConfirmationWhileEditingOneDoesnt() {
        val dispatched = mutableListOf<Invocation<*>>()
        val edit = Invocation(EditDocument, EditDocument.Input("README.md", "# Game"))
        uiState.dispatch(edit, onSuccess = {}) { invocation, _ -> dispatched += invocation }
        assertEquals(listOf<Invocation<*>>(edit), dispatched)
        listOf(
            Invocation(DeleteDocument, DeleteDocument.Input("README.md")),
            Invocation(AddDocumentToScreenplay, AddDocumentToScreenplay.Input("README.md", "README")),
        ).forEach { change ->
            uiState.dispatch(change, onSuccess = {}) { invocation, _ -> dispatched += invocation }
            assertSame(change, uiState.pendingDocumentChange?.invocation)
            assertNull(uiState.pendingDelete)
            uiState.finishDocumentChange()
            assertNull(uiState.pendingDocumentChange)
        }
        assertEquals(listOf<Invocation<*>>(edit), dispatched)
    }
}
