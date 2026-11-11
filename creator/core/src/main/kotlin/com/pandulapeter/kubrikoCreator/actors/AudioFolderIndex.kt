/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import java.io.File

/**
 * The audio folders of the game's own module ([AudioFolders.foldersBeside]) as of the last [refresh] and Kubriko
 * Creator's own changes since, so that validating a command reads nothing, while the changes themselves act on the
 * folders as they are.
 *
 * @param composeResources The `composeResources` folder new sounds and music go to.
 */
public class AudioFolderIndex(
    private val composeResources: () -> File,
) {
    private val lock = Any()

    @Volatile
    private var known: Set<String>? = null
    private val changesDuringWalk = ArrayList<(Set<String>) -> Set<String>>()

    /**
     * The folders, empty ones too, as of the last [refresh] and the changes since (before the first refresh, as they are
     * now, which walks them).
     */
    public fun folders(): Set<String> = known ?: AudioFolders.foldersBeside(composeResources())

    /**
     * Finds the folders again. Blocks, so callers run it off the main thread.
     */
    public fun refresh() {
        synchronized(lock) { changesDuringWalk.clear() }
        val walked = AudioFolders.foldersBeside(composeResources())
        synchronized(lock) { known = changesDuringWalk.fold(walked) { folders, change -> change(folders) } }
    }

    /**
     * Notes that the folder at [path], and those holding it, were made.
     */
    public fun onCreated(path: String): Unit = change { folders -> folders + setOf(path).withParents() }

    /**
     * Notes that the folder at [path], and those in it, are gone.
     */
    public fun onDeleted(path: String): Unit = change { folders -> folders.filterTo(sortedSetOf()) { !FolderPaths.isWithin(it, path) } }

    private fun change(change: (Set<String>) -> Set<String>) = synchronized(lock) {
        changesDuringWalk += change
        known = known?.let(change)
    }
}
