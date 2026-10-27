/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import org.jetbrains.compose.resources.StringResource

/**
 * What can be undone and redone, newest last.
 */
internal data class CommandHistory(
    val undoable: List<HistoryEntry> = emptyList(),
    val redoable: List<HistoryEntry> = emptyList(),
) {
    val nextUndoTitle: StringResource? get() = undoable.lastOrNull()?.title

    val nextRedoTitle: StringResource? get() = redoable.lastOrNull()?.title
}
