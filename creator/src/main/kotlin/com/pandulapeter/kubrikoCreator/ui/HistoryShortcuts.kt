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
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Whether this key event asks to undo (true), to redo (false), or for neither (null).
 */
internal fun KeyEvent.toHistoryStep() = if (type == KeyEventType.KeyDown) historyStep(key, isShortcutPressed = isMetaPressed || isCtrlPressed, isShiftPressed = isShiftPressed) else null

/**
 * Ctrl+Z (Cmd+Z on macOS) undoes; with Shift, or Ctrl+Y, it redoes.
 */
internal fun historyStep(key: Key, isShortcutPressed: Boolean, isShiftPressed: Boolean) = when {
    !isShortcutPressed -> null
    key == Key.Z -> !isShiftPressed
    key == Key.Y && !isShiftPressed -> false
    else -> null
}
