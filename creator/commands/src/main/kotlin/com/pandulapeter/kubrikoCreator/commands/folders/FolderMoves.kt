/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tells those who show folders that a command is about to move or rename one, whoever asked for it (the user, undo and
 * redo, or an AI), so that what they show inside it can follow it before the folder is shown where it went. Listeners are
 * called on the thread commands run on, before anything moves; a move that then fails moves nothing, so what followed it
 * is about a folder that doesn't exist.
 */
public class FolderMoves {
    private val listeners = CopyOnWriteArrayList<(FolderMove) -> Unit>()

    /**
     * Calls [listener] for every move from now on, until the returned handle is closed.
     */
    public fun listen(listener: (FolderMove) -> Unit): AutoCloseable {
        listeners += listener
        return AutoCloseable { listeners -= listener }
    }

    internal fun onMoving(move: FolderMove) = listeners.forEach { it(move) }
}

/**
 * The folder at [path] of [group] moving, with everything in it, to [newPath].
 */
public data class FolderMove(
    val group: FolderGroup,
    val path: String,
    val newPath: String,
)
