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

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.scenes.renameScene
import kotlinx.coroutines.withContext

@EditorMutation
internal suspend fun CommandContext.createFolder(group: FolderGroup, path: String) {
    when (val kind = group.kind) {
        null -> {
            withContext(ioDispatcher) { scenes.createFolder(path) }
            onScenesChanged(emptyList())
        }
        else -> requireBlockTypes.createFolder(kind, path)
    }
}

@EditorMutation
internal suspend fun CommandContext.deleteFolder(group: FolderGroup, path: String) {
    when (val kind = group.kind) {
        null -> {
            withContext(ioDispatcher) { scenes.deleteFolder(path) }
            onScenesChanged(emptyList())
        }
        else -> requireBlockTypes.deleteFolder(kind, path)
    }
}

/**
 * Moves the folder at [path] of [group], and everything in it, to [newPath].
 */
@EditorMutation
internal suspend fun CommandContext.moveFolder(group: FolderGroup, path: String, newPath: String) {
    fun moved(oldPath: String) = newPath + oldPath.removePrefix(path)
    foldersOf(group).filter { FolderPaths.isWithin(it, path) }.forEach { createFolder(group, moved(it)) }
    when (val kind = group.kind) {
        null -> scenes.names().filter { FolderPaths.isWithin(FolderPaths.parentOf(it), path) }.forEach { renameScene(it, moved(it)) }
        else -> requireBlockTypes.types.value.filter { it.kind == kind && FolderPaths.isWithin(it.folder, path) }.forEach { requireBlockTypes.put(it.withFolder(moved(it.folder))) }
    }
    deleteFolder(group, path)
}
