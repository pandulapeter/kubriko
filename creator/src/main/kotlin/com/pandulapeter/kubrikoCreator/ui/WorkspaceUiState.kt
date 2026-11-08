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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.screenplay.AddDocumentToScreenplay
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeleteDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.DeletePage
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettingsPage

/**
 * What the workspace screen shows besides the project: its side panels, its dialogs, and the AI step the user went back to.
 */
internal class WorkspaceUiState {
    var openPanel: SidePanel? by mutableStateOf(null)
        private set
    var aiPanelView by mutableStateOf(AiPanelView.ASSISTANT)
        private set
    var revisitedAiStep: RevisitedAiStep? by mutableStateOf(null)
        private set
    var isUpgradeNoticeDismissed by mutableStateOf(false)
        private set
    var isReviewingUpgrade by mutableStateOf(false)
        private set
    var isShowingProjectSettings by mutableStateOf(false)
        private set

    /**
     * The page Project settings opens on, or null for its first one.
     */
    var projectSettingsPage: ProjectSettingsPage? by mutableStateOf(null)
        private set

    /**
     * The delete the user is asked to confirm, if any.
     */
    var pendingDelete: PendingDelete? by mutableStateOf(null)
        private set

    /**
     * A change to one of the user's documents waiting for the user to confirm it.
     */
    var pendingDocumentChange: PendingDocumentChange? by mutableStateOf(null)
        private set

    /**
     * Opens [panel], or closes it if it is open.
     *
     * @return Whether version control just opened, which refreshes its state.
     */
    fun togglePanel(panel: SidePanel): Boolean {
        openPanel = panel.takeIf { it != openPanel }
        return openPanel == SidePanel.VERSION_CONTROL
    }

    fun showAiPanel() {
        openPanel = SidePanel.AI
    }

    /**
     * Opens the AI panel on the assistant, whichever view it showed.
     */
    fun showAssistant() {
        showAiPanel()
        aiPanelView = AiPanelView.ASSISTANT
    }

    fun selectAiPanelView(view: AiPanelView) {
        aiPanelView = view
    }

    /**
     * Shows [step] again, until the AI makes a change after [latestStepId].
     */
    fun revisit(step: AiStep, latestStepId: Int?) {
        revisitedAiStep = RevisitedAiStep(step, latestStepId = latestStepId)
    }

    fun dismissUpgradeNotice() {
        isUpgradeNoticeDismissed = true
    }

    /**
     * Shows the template upgrade, from the notice or from Project settings, which it closes.
     */
    fun reviewUpgrade() {
        isShowingProjectSettings = false
        isReviewingUpgrade = true
    }

    /**
     * Closes the template upgrade, applied or dismissed.
     */
    fun finishReviewingUpgrade() {
        isReviewingUpgrade = false
    }

    /**
     * Opens Project settings on [page], or on its first page.
     */
    fun showProjectSettings(page: ProjectSettingsPage? = null) {
        projectSettingsPage = page
        isShowingProjectSettings = true
    }

    fun hideProjectSettings() {
        isShowingProjectSettings = false
    }

    /**
     * Dispatches [invocation] with [onSuccess] through [dispatch], or, for a delete, first asks the user to confirm it
     * ([pendingDelete]), so that the user sees what it changes wherever the delete starts, and for a change that deletes
     * or moves one of the user's documents, asks too ([pendingDocumentChange]).
     */
    fun dispatch(invocation: Invocation<*>, onSuccess: () -> Unit, dispatch: (Invocation<*>, () -> Unit) -> Unit) {
        when (invocation.command) {
            is DeletingCommand<*> -> pendingDelete = PendingDelete(invocation, onSuccess)
            DeleteDocument, AddDocumentToScreenplay, DeletePage -> confirmDocumentChange(invocation) { onSuccess() }
            else -> dispatch(invocation, onSuccess)
        }
    }

    /**
     * Closes the delete's confirmation, confirmed or not.
     */
    fun finishDeleting() {
        pendingDelete = null
    }

    /**
     * Asks the user to confirm [invocation], a change to one of their documents, running [onSuccess] with the change as
     * confirmed once it was applied.
     */
    fun confirmDocumentChange(invocation: Invocation<*>, onSuccess: (Invocation<*>) -> Unit) {
        pendingDocumentChange = PendingDocumentChange(invocation, onSuccess)
    }

    /**
     * Closes the document change's confirmation, confirmed or not.
     */
    fun finishDocumentChange() {
        pendingDocumentChange = null
    }
}
