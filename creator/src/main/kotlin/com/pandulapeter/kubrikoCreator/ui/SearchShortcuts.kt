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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

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
 * Opens what this key event's search shortcut asks for in [uiState], Shift pressed twice ([WorkspaceUiState.doubleShift])
 * among them.
 *
 * @return Whether it was one.
 */
internal fun KeyEvent.openSearch(uiState: WorkspaceUiState): Boolean {
    val isDown = type == KeyEventType.KeyDown
    if (type != KeyEventType.KeyUp && !isDown) return false
    val shortcut = when {
        uiState.doubleShift.onKey(key, isDown) -> SearchShortcut.QUICK_OPEN
        isDown -> searchShortcutOf(key, isShortcutPressed = isMetaPressed || isCtrlPressed, isShiftPressed = isShiftPressed, isAltPressed = isAltPressed)
        else -> null
    }
    when (shortcut) {
        SearchShortcut.FILTER_SIDEBAR -> uiState.sidebarFilterFocus.request()
        SearchShortcut.QUICK_OPEN -> uiState.showQuickOpen()
        null -> Unit
    }
    return shortcut != null
}
