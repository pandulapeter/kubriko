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

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.resources.resourceFilesOnDisk
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.validatePageName
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext

/**
 * Why the folder at [path] of [group], an existing one, can't be at [newPath] instead: as a new folder there, or because
 * a page or a folder in it would get a name past the limit.
 */
internal fun CommandContext.validateFolderRelocation(group: FolderGroup, path: String, newPath: String): Rejection? =
    validateNewFolder(group, newPath) ?: validateRelocatedPages(group, path, newPath)

private fun CommandContext.validateRelocatedPages(group: FolderGroup, path: String, newPath: String): Rejection? = if (group == FolderGroup.PAGES) {
    (itemsOf(FolderGroup.PAGES).filter { (_, folder) -> FolderPaths.isWithin(folder, path) }.map { (page, _) -> page } +
        foldersOf(FolderGroup.PAGES).filter { it != path && FolderPaths.isWithin(it, path) })
        .firstNotNullOfOrNull { page -> validatePageName(newPath + page.removePrefix(path)) }
} else {
    null
}

/**
 * Moves the folder at [path] of [group], with everything in it, to [newPath], as renaming and moving a folder both do: a
 * Scene's or audio file's name is its path, so what uses it follows, and so do the screenplay's mentions of everything in it.
 *
 * @param movesOnly Whether an audio folder's files only move, leaving what uses them (how undo and redo move them).
 * @param back What moves it back from [newPath], moving only an audio folder's files when given true.
 */
@EditorMutation
internal suspend fun CommandContext.relocateFolder(
    group: FolderGroup,
    path: String,
    newPath: String,
    movesOnly: Boolean,
    back: (movesOnly: Boolean) -> Invocation<*>,
): Applied {
    val scenesBefore = if (group == FolderGroup.SCENES) sceneFilesWithin(path) else emptyMap()
    val (audioMoves, audioRenames) = if (group.isAudio) {
        withContext(ioDispatcher) {
            audioFolderMoves(path, newPath) to audioFilesMovedWith(resourceFilesOnDisk().map { it.path }.distinct(), path, newPath)
        }
    } else {
        emptyList<PathMove>() to emptyMap()
    }
    moveFolder(group, path, newPath)?.let { rejection -> return Applied(inverse = null, failure = rejection) }
    val inverse = if (!group.isAudio) {
        back(false)
    } else {
        // Restoring, since the resource scan that knows the moved files may come after the undo.
        val restoring = listOf(back(true).copy(isRestoring = true)) + if (movesOnly) emptyList() else followAudioFiles(audioRenames)
        restoring.singleOrNull() ?: Invocation(CompositeCommand, CompositeCommand.Input(restoring))
    }
    val moves = when (group) {
        FolderGroup.PAGES -> listOf(PathMove("${PageNaming.FOLDER}/$path", "${PageNaming.FOLDER}/$newPath"))
        FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> checkNotNull(group.kind).let { kind ->
            listOf(PathMove(DefinitionPaths.folderPath(kind, path), DefinitionPaths.folderPath(kind, newPath)))
        }
        FolderGroup.SCENES -> withContext(ioDispatcher) {
            scenesBefore.map { (scene, file) -> PathMove(file, projectPathOf(scenes.fileOf(newPath + scene.removePrefix(path)))) }
        }
        FolderGroup.SPRITES -> emptyList()
        FolderGroup.SOUNDS, FolderGroup.MUSIC -> audioMoves
    }
    return withMentionsFollowed(moves, Applied(inverse = inverse))
}

/**
 * The file of every Scene in [folder], by the Scene's name: a folder of Scenes may be in more than one module.
 */
private suspend fun CommandContext.sceneFilesWithin(folder: String) = withContext(ioDispatcher) {
    scenes.scanNames().filter { FolderPaths.isWithin(FolderPaths.parentOf(it), folder) }.associateWith { projectPathOf(scenes.fileOf(it)) }
}
