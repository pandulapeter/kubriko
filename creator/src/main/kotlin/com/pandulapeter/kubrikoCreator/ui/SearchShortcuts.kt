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
}

/**
 * The search [key] with Cmd/Ctrl ([isShortcutPressed]) and Shift asks for, if any.
 */
internal fun searchShortcutOf(key: Key, isShortcutPressed: Boolean, isShiftPressed: Boolean, isAltPressed: Boolean) = when {
    !isShortcutPressed || isAltPressed -> null
    key == Key.F && !isShiftPressed -> SearchShortcut.FILTER_SIDEBAR
    else -> null
}

/**
 * Opens what this key event's search shortcut asks for in [uiState].
 *
 * @return Whether it was one.
 */
internal fun KeyEvent.openSearch(uiState: WorkspaceUiState): Boolean {
    if (type != KeyEventType.KeyDown) return false
    return when (searchShortcutOf(key, isShortcutPressed = isMetaPressed || isCtrlPressed, isShiftPressed = isShiftPressed, isAltPressed = isAltPressed)) {
        SearchShortcut.FILTER_SIDEBAR -> true.also { uiState.focusSidebarFilter() }
        null -> false
    }
}
