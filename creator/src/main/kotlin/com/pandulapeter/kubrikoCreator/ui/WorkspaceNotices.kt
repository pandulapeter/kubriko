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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.ui.mcp.AiActivityBar
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * The bars under the workspace's toolbar: the AI's activity, why the project can't be changed, and its template upgrade.
 *
 * @param readOnlyReason Why the project may not be changed, if it may not.
 * @param isUpgradeNeeded Whether the project's template files can be upgraded.
 */
@Composable
internal fun WorkspaceNotices(
    uiState: WorkspaceUiState,
    aiSteps: List<AiStep>,
    readOnlyReason: Rejection?,
    isUpgradeNeeded: Boolean,
) {
    val aiActivitySettings = LocalAiActivitySettings.current
    AiActivityBar(
        steps = aiSteps,
        isFollowing = aiActivitySettings.isFollowing,
        onFollowingChange = aiActivitySettings.onFollowingChange,
        onClick = uiState::showAiPanel,
    )
    var shownReadOnlyReason by remember { mutableStateOf(readOnlyReason) }
    if (readOnlyReason != null) shownReadOnlyReason = readOnlyReason
    AnimatedVisibility(
        visible = readOnlyReason != null,
        enter = NoticeEnter,
        exit = NoticeExit,
    ) {
        shownReadOnlyReason?.let { ReadOnlyProjectNotice(reason = it) }
    }
    AnimatedVisibility(
        visible = isUpgradeNeeded && !uiState.isUpgradeNoticeDismissed,
        enter = NoticeEnter,
        exit = NoticeExit,
    ) {
        ProjectUpgradeNotice(
            onReview = uiState::reviewUpgrade,
            onDismiss = uiState::dismissUpgradeNotice,
        )
    }
}

private val NoticeEnter = expandVertically(CreatorMotion.medium()) + fadeIn(CreatorMotion.medium())

private val NoticeExit = shrinkVertically(CreatorMotion.medium()) + fadeOut(CreatorMotion.short())
