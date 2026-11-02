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

import androidx.compose.runtime.Composable
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.ui.mcp.AiActivityBar

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
    AiActivityBar(
        steps = aiSteps,
        isFollowing = uiState.isFollowingAi,
        onFollowingChange = uiState::followAi,
        onClick = uiState::showAiPanel,
    )
    readOnlyReason?.let { ReadOnlyProjectNotice(reason = it) }
    if (isUpgradeNeeded && !uiState.isUpgradeNoticeDismissed) {
        ProjectUpgradeNotice(
            onReview = uiState::reviewUpgrade,
            onDismiss = uiState::dismissUpgradeNotice,
        )
    }
}
