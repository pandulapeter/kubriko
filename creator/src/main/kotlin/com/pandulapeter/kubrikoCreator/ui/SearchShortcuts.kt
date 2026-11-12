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

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The ways of finding something in the workspace that a shortcut opens.
 */
internal enum class SearchShortcut {
    /** Cmd/Ctrl+F: the sidebar's filter field. */
    FILTER_SIDEBAR,

    /** Cmd/Ctrl+P, or Shift twice ([DoubleShift]): quick open. */
    QUICK_OPEN,
}

/**
 * The search [key] with Cmd/Ctrl ([isShortcutPressed]), Shift, and Alt asks for, if any: none with Alt, which the
 * system's own shortcuts use.
 */
internal fun searchShortcutOf(key: Key, isShortcutPressed: Boolean, isShiftPressed: Boolean, isAltPressed: Boolean) = when {
    !isShortcutPressed || isAltPressed -> null
    key == Key.F && !isShiftPressed -> SearchShortcut.FILTER_SIDEBAR
    key == Key.P && !isShiftPressed -> SearchShortcut.QUICK_OPEN
    else -> null
}

/**
 * Opens what the search shortcuts ask for in [uiState] ([openSearch]), seeing every key before the focused element does
 * and every press of a pointer inside, which breaks a double Shift.
 */
internal fun Modifier.searchShortcuts(uiState: WorkspaceUiState): Modifier = onPreviewKeyEvent { event -> event.openSearch(uiState) }
    .pointerInput(uiState) {
        awaitPointerEventScope {
            while (true) {
                if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) uiState.doubleShift.onPointerPressed()
            }
        }
    }

/**
 * Opens what this key event's search shortcut asks for in [uiState], Shift pressed twice ([WorkspaceUiState.doubleShift])
 * among them.
 *
 * @return Whether the event was taken: a Cmd/Ctrl shortcut is, while a Shift press is left for the focused element too,
 *   which would otherwise see Shift released without seeing it pressed.
 */
internal fun KeyEvent.openSearch(uiState: WorkspaceUiState): Boolean {
    val isDown = type == KeyEventType.KeyDown
    if (type != KeyEventType.KeyUp && !isDown) return false
    if (uiState.doubleShift.onKey(key, isDown)) {
        uiState.showQuickOpen()
        return false
    }
    return when (if (isDown) searchShortcutOf(key, isShortcutPressed = isMetaPressed || isCtrlPressed, isShiftPressed = isShiftPressed, isAltPressed = isAltPressed) else null) {
        SearchShortcut.FILTER_SIDEBAR -> true.also { uiState.sidebarFilterFocus.request() }
        SearchShortcut.QUICK_OPEN -> true.also { uiState.showQuickOpen() }
        null -> false
    }
}
