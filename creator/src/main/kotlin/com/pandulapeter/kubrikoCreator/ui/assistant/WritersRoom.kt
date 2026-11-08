/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Starts a conversation in the writers' room with a prompt, or null outside a workspace: what "Build this with the
 * assistant" on a Planned item, and the AI panel's "Continue in the writers' room" and "Plan a feature", do.
 */
internal val LocalWritersRoom = compositionLocalOf<((prompt: String) -> Unit)?> { null }

/**
 * What [LocalWritersRoom] provides in a workspace: [onShowAssistant], so that the conversation is seen, then [onStart].
 */
@Composable
internal fun rememberWritersRoom(onShowAssistant: () -> Unit, onStart: (prompt: String) -> Unit): (prompt: String) -> Unit {
    val currentOnShowAssistant by rememberUpdatedState(onShowAssistant)
    val currentOnStart by rememberUpdatedState(onStart)
    return remember {
        { prompt ->
            currentOnShowAssistant()
            currentOnStart(prompt)
        }
    }
}
