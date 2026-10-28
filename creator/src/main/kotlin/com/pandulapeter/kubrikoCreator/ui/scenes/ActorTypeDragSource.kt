/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Lets the element be dragged as the Actor type [actorTypeName] into the open Scene, reporting the pointer to [state]
 * in root coordinates.
 */
@Composable
internal fun Modifier.actorTypeDragSource(state: ActorTypeDragState, actorTypeName: String): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }.pointerInput(state, actorTypeName) {
        detectDragGestures(
            onDragStart = { state.start(actorTypeName, origin + it) },
            onDragEnd = state::end,
            onDragCancel = state::cancel,
            onDrag = { change, _ ->
                change.consume()
                state.moveTo(origin + change.position)
            },
        )
    }
}
