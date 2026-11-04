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

import androidx.compose.runtime.compositionLocalOf
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings

/**
 * The Creator settings that shape how the AI's work shows in the workspace, provided once above it.
 *
 * @param isFollowing Whether the editor goes to what each of the AI's changes is about.
 * @param followDelayInMillis How long the editor shows one of the AI's changes at least before it goes to the next one.
 * @param isTurnReviewShown Whether the assistant ends each turn that changed the project with Keep and Undo.
 * @param onFollowingChange Turns following the AI on or off, for good.
 */
internal data class AiActivitySettings(
    val isFollowing: Boolean,
    val followDelayInMillis: Int,
    val isTurnReviewShown: Boolean,
    val onFollowingChange: (Boolean) -> Unit,
) {
    companion object {
        fun of(settings: CreatorSettings, onFollowingChange: (Boolean) -> Unit) = AiActivitySettings(
            isFollowing = settings.isFollowingAi,
            followDelayInMillis = settings.aiFollowDelayInMillis,
            isTurnReviewShown = settings.isTurnReviewShown,
            onFollowingChange = onFollowingChange,
        )
    }
}

internal val LocalAiActivitySettings = compositionLocalOf { AiActivitySettings.of(CreatorSettings(), onFollowingChange = {}) }
