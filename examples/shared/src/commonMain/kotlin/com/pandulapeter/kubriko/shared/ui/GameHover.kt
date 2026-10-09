/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

/**
 * Calls [onEnter] when a hovering pointer enters the element and [onExit] when it leaves.
 *
 * The games use this to highlight their buttons and play a hover sound. The latest callbacks are
 * always the ones invoked, even when their identity changes between compositions.
 */
fun Modifier.gameHover(
    onEnter: () -> Unit,
    onExit: () -> Unit = {},
): Modifier = this then GameHoverElement(
    onEnter = onEnter,
    onExit = onExit,
)

private data class GameHoverElement(
    val onEnter: () -> Unit,
    val onExit: () -> Unit,
) : ModifierNodeElement<GameHoverNode>() {

    override fun create() = GameHoverNode(
        onEnter = onEnter,
        onExit = onExit,
    )

    override fun update(node: GameHoverNode) {
        node.onEnter = onEnter
        node.onExit = onExit
    }
}

private class GameHoverNode(
    var onEnter: () -> Unit,
    var onExit: () -> Unit,
) : Modifier.Node(), PointerInputModifierNode {

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Main) {
            when (pointerEvent.type) {
                PointerEventType.Enter -> onEnter()
                PointerEventType.Exit -> onExit()
            }
        }
    }

    override fun onCancelPointerInput() = Unit
}
