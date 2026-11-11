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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor

/**
 * Undo and redo steps an editor keeps of its own before they become the workspace's (the sprite editor's draft), which
 * the toolbar's buttons and the shortcuts take first while it has some.
 */
internal interface EditorHistory {
    val canUndo: Boolean
    val canRedo: Boolean

    /**
     * Undoes the editor's last step, returning whether it had one.
     */
    fun undo(): Boolean

    /**
     * Redoes the editor's last undone step, returning whether it had one.
     */
    fun redo(): Boolean
}

/**
 * Undoes (when [isUndo]) or redoes a step of this editor's own, or one of the workspace's [commands] when it has none.
 */
internal fun EditorHistory?.step(isUndo: Boolean, commands: CommandExecutor) {
    val isDone = this?.let { if (isUndo) it.undo() else it.redo() } == true
    if (!isDone) if (isUndo) commands.undo() else commands.redo()
}
