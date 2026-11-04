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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.scenes.renameScene
import com.pandulapeter.kubrikoCreator.commands.scenes.sceneNotMoved
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import kotlinx.coroutines.withContext

@EditorMutation
internal suspend fun CommandContext.createFolder(group: FolderGroup, path: String) {
    when (group) {
        FolderGroup.SCENES -> withContext(ioDispatcher) { scenes.createFolder(path) }
        FolderGroup.PAGES -> withContext(ioDispatcher) { screenplay.createFolder(path) }
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> requireBlockTypes.createFolder(checkNotNull(group.kind), path)
    }
}

@EditorMutation
internal suspend fun CommandContext.deleteFolder(group: FolderGroup, path: String) {
    when (group) {
        FolderGroup.SCENES -> withContext(ioDispatcher) { scenes.deleteFolder(path) }
        FolderGroup.PAGES -> withContext(ioDispatcher) { screenplay.deleteFolder(path) }
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> requireBlockTypes.deleteFolder(checkNotNull(group.kind), path)
    }
}

/**
 * Moves the folder at [path] of [group], and everything in it, to [newPath]. It stops at the first Scene or page that
 * can't be moved, moving back the ones moved before it, so that nothing changes.
 *
 * @return Why it couldn't be moved (the Scene or page that couldn't), or null if everything was.
 */
@EditorMutation
internal suspend fun CommandContext.moveFolder(group: FolderGroup, path: String, newPath: String): Rejection? {
    fun moved(oldPath: String) = newPath + oldPath.removePrefix(path)
    foldersOf(group).filter { FolderPaths.isWithin(it, path) }.forEach { createFolder(group, moved(it)) }
    when (group) {
        FolderGroup.SCENES -> {
            val movedScenes = ArrayList<String>()
            withContext(ioDispatcher) { scenes.scanNames() }.filter { FolderPaths.isWithin(FolderPaths.parentOf(it), path) }.forEach { scene ->
                if (!renameScene(scene, moved(scene))) {
                    movedScenes.asReversed().forEach { renameScene(moved(it), it) }
                    deleteFolder(group, newPath)
                    return sceneNotMoved(scene)
                }
                movedScenes += scene
            }
        }
        FolderGroup.PAGES -> withContext(ioDispatcher) {
            val movedPages = ArrayList<String>()
            screenplay.scanNames().filter { FolderPaths.isWithin(PageNaming.folderOf(it), path) }.forEach { page ->
                if (!screenplay.move(page, moved(page))) {
                    movedPages.asReversed().forEach { screenplay.move(moved(it), it) }
                    screenplay.deleteFolder(newPath)
                    return@withContext Rejection(CommandProblem.PAGE_NOT_MOVED, page)
                }
                movedPages += page
            }
            null
        }?.let { return it }
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> requireBlockTypes.types.value
            .filter { it.kind == group.kind && FolderPaths.isWithin(it.folder, path) }
            .forEach { requireBlockTypes.put(it.withFolder(moved(it.folder))) }
    }
    deleteFolder(group, path)
    return null
}
