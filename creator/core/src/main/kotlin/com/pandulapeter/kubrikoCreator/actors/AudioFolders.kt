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

import com.pandulapeter.kubrikoCreator.project.ProjectTree
import java.io.File

/**
 * The real folders sounds and music are kept in: those of the `files` folder of a module's `composeResources` folders,
 * named by their path from it (`sounds/enemies`, [ResourceKind.audioFolderOf]), since the game reads its audio by path.
 */
public object AudioFolders {

    /**
     * The `files` folder of every source set of the module whose `composeResources` folder is [composeResources]
     * (`src/<source set>/composeResources/files`), the one of [composeResources] first, whether it exists or not.
     */
    public fun filesDirectoriesBeside(composeResources: File): List<File> {
        val own = File(composeResources, FILES)
        val sources = composeResources.parentFile?.parentFile ?: return listOf(own)
        val others = sources.listFiles().orEmpty()
            .map { File(File(it, ResourceKind.COMPOSE_RESOURCES), FILES) }
            .filter { it.isDirectory && it.absoluteFile != own.absoluteFile }
            .sortedBy { it.path }
        return listOf(own) + others
    }

    /**
     * Every folder of the `files` folders of [composeResources]' module ([filesDirectoriesBeside]), empty ones too, but
     * the Scenes folder and those in it. Blocks.
     */
    public fun foldersBeside(composeResources: File): Set<String> = filesDirectoriesBeside(composeResources)
        .filter { it.isDirectory }
        .flatMapTo(sortedSetOf()) { files ->
            ProjectTree.walk(files)
                .filter { it.isDirectory && it != files }
                .map { it.relativeTo(files).invariantSeparatorsPath }
                .filterNot(ResourceKind::isScenesFolder)
                .toList()
        }

    /**
     * The folders among [folders] that the group of [kind] shows: those holding its files, and of the others, those
     * whose nearest folder holding any audio holds its files too, or that no such folder holds. So `music` isn't among
     * the sounds' folders, nor an empty folder in it, while an empty folder at the top is in both.
     *
     * @param audio The sounds and music, by their paths from `composeResources`.
     */
    public fun ofKind(kind: ResourceKind, folders: Set<String>, audio: AudioFiles): Set<String> {
        val own = foldersHolding(if (kind == ResourceKind.MUSIC) audio.music else audio.sounds)
        val others = foldersHolding(if (kind == ResourceKind.MUSIC) audio.sounds else audio.music)
        return folders.filterTo(sortedSetOf()) { folder ->
            val holding = generateSequence(folder) { FolderPaths.parentOf(it).ifEmpty { null } }.firstOrNull { it in own || it in others }
            holding == null || holding in own
        }
    }

    private fun foldersHolding(paths: List<String>) = paths.mapNotNullTo(HashSet()) { ResourceKind.audioFolderOf(it).ifEmpty { null } }.withParents()

    private const val FILES = "files"
}
