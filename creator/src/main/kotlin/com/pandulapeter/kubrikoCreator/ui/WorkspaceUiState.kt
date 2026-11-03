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

/**
 * What the workspace screen shows besides the project: its side panels, its dialogs, and whether it follows the AI.
 *
 * @param initialAiPanelView The view the AI panel opens with.
 */
internal class WorkspaceUiState(
    initialAiPanelView: AiPanelView,
) {
    var openPanel: SidePanel? by mutableStateOf(null)
        private set
    var aiPanelView by mutableStateOf(initialAiPanelView)
        private set
    var revisitedAiStep: RevisitedAiStep? by mutableStateOf(null)
        private set
    var isFollowingAi by mutableStateOf(true)
        private set
    var isUpgradeNoticeDismissed by mutableStateOf(false)
        private set
    var isReviewingUpgrade by mutableStateOf(false)
        private set
    var isShowingProjectSettings by mutableStateOf(false)
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

    fun selectAiPanelView(view: AiPanelView) {
        aiPanelView = view
    }

    fun followAi(isFollowing: Boolean) {
        isFollowingAi = isFollowing
    }

    /**
     * Shows [step] again, until the AI takes a step after [latestStepId].
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

    fun showProjectSettings() {
        isShowingProjectSettings = true
    }

    fun hideProjectSettings() {
        isShowingProjectSettings = false
    }
}
