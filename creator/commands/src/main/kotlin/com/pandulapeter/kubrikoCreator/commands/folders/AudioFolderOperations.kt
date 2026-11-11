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

import com.pandulapeter.kubrikoCreator.actors.AudioFolders
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.resources.followInScenes
import com.pandulapeter.kubrikoCreator.commands.resources.followInTypes
import com.pandulapeter.kubrikoCreator.commands.resources.knownResourcePaths
import com.pandulapeter.kubrikoCreator.commands.resources.reportNamedInCode
import com.pandulapeter.kubrikoCreator.commands.resources.resourceFilesOnDisk
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.context.withKotlinFilesHeld
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import java.io.File

/**
 * The `files` folders of the game's own module, one per source set ([AudioFolders.filesDirectoriesBeside]), the one new
 * files go to first.
 */
private fun CommandContext.filesDirectories() = AudioFolders.filesDirectoriesBeside(scenes.resourcesDirectory)

/**
 * Makes the audio folder at [path] in the `files` folder new sounds and music go to. Blocks.
 */
@EditorMutation
internal fun CommandContext.createAudioFolder(path: String) {
    if (File(filesDirectories().first(), path).mkdirs()) {
        audioFolders.onCreated(path)
        onFilesChanged()
    }
}

/**
 * Deletes the audio folder at [path] from every source set that has it and holds no file in it any more, so one that
 * still holds a file nothing lists (a `.DS_Store`, or a file the game reads by its own path) stays. Blocks.
 */
@EditorMutation
internal fun CommandContext.deleteAudioFolder(path: String) {
    val folders = filesDirectories().map { File(it, path) }
    val deleted = folders.count { folder ->
        folder.isDirectory && ProjectTree.walkWithinSources(folder).none { it.isFile } && folder.deleteRecursively()
    }
    if (deleted > 0) {
        if (folders.none { it.exists() }) audioFolders.onDeleted(path)
        onFilesChanged()
    }
}

/**
 * Moves every file and folder in the audio folder at [path] to [newPath], in every source set that has it, moving back
 * those moved already when one can't be (another program has it open, or one is already there), so that nothing
 * changes then. The folder it leaves is deleted afterwards ([deleteAudioFolder]). Blocks.
 *
 * @return Why it couldn't be moved, or null if everything was.
 */
@EditorMutation
internal fun CommandContext.moveAudioFolder(path: String, newPath: String): Rejection? {
    val moved = ArrayList<Pair<File, File>>()
    filesDirectories().forEach { files ->
        val from = File(files, path)
        if (!from.isDirectory) return@forEach
        val to = File(files, newPath)
        val contents = ProjectTree.walkWithinSources(from).filter { it != from }.toList()
        to.mkdirs()
        audioFolders.onCreated(newPath)
        contents.filter { it.isDirectory }.forEach { folder ->
            File(to, folder.relativeTo(from).path).mkdirs()
            audioFolders.onCreated(FolderPaths.child(newPath, folder.relativeTo(from).invariantSeparatorsPath))
        }
        contents.filter { it.isFile }.forEach { file ->
            val target = File(to, file.relativeTo(from).path)
            if (target.exists() || !file.moveWithoutReplacing(target)) {
                moved.asReversed().forEach { (source, destination) -> destination.moveWithoutReplacing(source) }
                deleteAudioFolder(newPath)
                return Rejection(CommandProblem.RESOURCE_FOLDER_NOT_MOVED, path)
            }
            scenes.ownWrites.record(file)
            scenes.ownWrites.record(target)
            moved += file to target
        }
    }
    onFilesChanged()
    return null
}

/**
 * How the audio folder at [path] moves to [newPath], by the folders' paths from the project's root, one per source set
 * that has it, as the screenplay's mentions of what is in it follow.
 */
internal fun CommandContext.audioFolderMoves(path: String, newPath: String): List<PathMove> = filesDirectories()
    .filter { File(it, path).isDirectory }
    .map { PathMove(projectPathOf(File(it, path)), projectPathOf(File(it, newPath))) }

/**
 * The sounds and music among [paths] (from `composeResources`) in the audio folder at [path] and the folders in it,
 * each with the path it gets once the folder is at [newPath].
 */
internal fun audioFilesMovedWith(paths: Collection<String>, path: String, newPath: String): Map<String, String> = paths
    .filter { isAudioWithin(it, path) }
    .associateWith { ResourceKind.inAudioFolder(it, newPath + ResourceKind.audioFolderOf(it).removePrefix(path)) }

/**
 * The sounds and music in the audio folder at [path] and the folders in it, as the last resource scan knows them.
 */
internal fun CommandContext.knownAudioFilesWithin(path: String): List<String> = knownResourcePaths().orEmpty().filter { isAudioWithin(it, path) }

/**
 * Whether a sound or music file is in the audio folder at [path] or a folder in it, in any source set. Blocks.
 */
internal fun CommandContext.holdsAudioOnDisk(path: String): Boolean = resourceFilesOnDisk().any { isAudioWithin(it.path, path) }

private fun isAudioWithin(resourcePath: String, folder: String): Boolean {
    val kind = ResourceKind.of(resourcePath)
    return kind != null && kind != ResourceKind.SPRITE && FolderPaths.isWithin(ResourceKind.audioFolderOf(resourcePath), folder)
}

/**
 * Makes what uses each sound or music file of [renames] (from `composeResources`) use its new path, as renaming each
 * would, in one pass: the hand-written Kotlin read once, and the Scenes written together.
 *
 * @return What puts back the types and Scenes it changed exactly, or nothing if it changed none.
 */
@EditorMutation
internal suspend fun CommandContext.followAudioFiles(renames: Map<String, String>): List<Invocation<*>> {
    if (renames.isEmpty()) return emptyList()
    withKotlinFilesHeld { renames.forEach { (path, newPath) -> reportNamedInCode(path, newPath) } }
    return listOfNotNull(followInTypes(renames), followInScenes(renames))
}
