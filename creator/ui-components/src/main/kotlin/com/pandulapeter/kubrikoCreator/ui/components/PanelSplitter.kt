/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.panel_resize
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.panel_resize_height
import org.jetbrains.compose.resources.stringResource
import java.awt.Cursor

/**
 * The edge between a side panel and what is beside it: a divider that can be dragged to resize the panel (on the [side]
 * of the divider it is on) and double-clicked to give it its default width again.
 *
 * @param onDrag Called with how much the panel grows, in dp (negative when it shrinks).
 */
@Composable
public fun PanelSplitter(
    onDrag: (deltaInDp: Float) -> Unit,
    onDragStopped: () -> Unit,
    onReset: () -> Unit,
    side: PanelSide,
) {
    val density = LocalDensity.current.density
    val isVertical = side == PanelSide.BOTTOM
    val description = stringResource(if (isVertical) Res.string.panel_resize_height else Res.string.panel_resize)
    Box(
        modifier = Modifier
            .then(if (isVertical) Modifier.height(8.dp).fillMaxWidth() else Modifier.width(8.dp).fillMaxHeight())
            .pointerHoverIcon(PointerIcon(Cursor(if (isVertical) Cursor.N_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR)))
            .draggable(
                state = rememberDraggableState { deltaInPixels ->
                    val deltaInDp = deltaInPixels / density
                    onDrag(if (side == PanelSide.START) deltaInDp else -deltaInDp)
                },
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                onDragStopped = { onDragStopped() },
            )
            .pointerInput(onReset) { detectTapGestures(onDoubleTap = { onReset() }) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (isVertical) HorizontalDivider() else VerticalDivider()
    }
}

/**
 * The [PanelSplitter] of the panel whose width [state] holds, in [row].
 */
@Composable
public fun PanelSplitter(
    state: PanelWidthState,
    row: PanelRow,
): Unit = PanelSplitter(
    onDrag = { state.onDrag(it, row) },
    onDragStopped = state::onDragStopped,
    onReset = state::onReset,
    side = state.panel.side,
)

/**
 * The [PanelSplitter] above the panel whose height [state] holds, in a column [availableHeight] tall. [onDrag] hears every
 * drag before the panel follows it (a closed panel opens on it).
 */
@Composable
public fun PanelSplitter(
    state: PanelHeightState,
    availableHeight: Dp,
    onDrag: () -> Unit = {},
): Unit = PanelSplitter(
    onDrag = { growth ->
        onDrag()
        state.onDrag(growth, availableHeight)
    },
    onDragStopped = state::onDragStopped,
    onReset = state::onReset,
    side = state.panel.side,
)
