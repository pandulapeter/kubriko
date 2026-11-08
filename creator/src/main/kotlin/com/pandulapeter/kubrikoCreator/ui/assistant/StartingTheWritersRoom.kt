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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.WritersRoomRequest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_prompt_writers_room_idea
import org.jetbrains.compose.resources.stringResource

/**
 * Starts the writers' room with the idea [request] holds, once, showing the assistant: what a project created from an
 * idea opens on.
 */
@Composable
internal fun StartingTheWritersRoom(
    request: WritersRoomRequest?,
    onShowAssistant: () -> Unit,
    onStart: (prompt: String) -> Unit,
) {
    val pitch = remember(request) { request?.take() } ?: return
    val prompt = stringResource(Res.string.assistant_prompt_writers_room_idea, pitch)
    val currentOnShowAssistant by rememberUpdatedState(onShowAssistant)
    val currentOnStart by rememberUpdatedState(onStart)
    LaunchedEffect(prompt) {
        currentOnShowAssistant()
        currentOnStart(prompt)
    }
}
