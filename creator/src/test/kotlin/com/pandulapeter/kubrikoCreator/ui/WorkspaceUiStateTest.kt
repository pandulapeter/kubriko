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
    fun reviewingTheUpgradeFromProjectSettingsClosesTheSettings() {
        uiState.showProjectSettings()
        uiState.reviewUpgrade()
        assertFalse(uiState.isShowingProjectSettings)
        assertTrue(uiState.isReviewingUpgrade)
        uiState.finishReviewingUpgrade()
        assertFalse(uiState.isReviewingUpgrade)
    }

    @Test
    fun dismissingTheUpgradeNoticeAndFollowingTheAiAreRemembered() {
        assertFalse(uiState.isUpgradeNoticeDismissed)
        uiState.dismissUpgradeNotice()
        assertTrue(uiState.isUpgradeNoticeDismissed)
        assertTrue(uiState.isFollowingAi)
        uiState.followAi(false)
        assertFalse(uiState.isFollowingAi)
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
}
