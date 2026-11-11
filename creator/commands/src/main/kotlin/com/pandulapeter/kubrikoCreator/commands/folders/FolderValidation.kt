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

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.AudioFolders
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.withParents
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.resources.knownResourcePaths
import com.pandulapeter.kubrikoCreator.commands.screenplay.validatePageName
import com.pandulapeter.kubrikoCreator.scenes.SceneNaming
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * Every folder of [group], as paths (`enemies/flying`), parents included.
 */
internal fun CommandContext.foldersOf(group: FolderGroup): Set<String> = when (group) {
    FolderGroup.SCENES -> (scenes.folders() + scenes.names().map(FolderPaths::parentOf).filter { it.isNotEmpty() }).withParents()
    FolderGroup.PAGES -> (screenplay.folders() + screenplay.names().map(PageNaming::folderOf).filter { it.isNotEmpty() }).withParents()
    FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> blockTypes?.folders?.value?.get(checkNotNull(group.kind)).orEmpty()
    FolderGroup.SOUNDS, FolderGroup.MUSIC -> AudioFolders.ofKind(checkNotNull(group.resourceKind), audioFolders.folders(), knownAudioFiles())
}

/**
 * The names of the types, Scenes, or pages of [group] and the folders they are in; for sounds and music, their paths
 * from `composeResources` and their folders from `composeResources/files`.
 */
internal fun CommandContext.itemsOf(group: FolderGroup): List<Pair<String, String>> = when (group) {
    FolderGroup.SCENES -> scenes.names().map { it to FolderPaths.parentOf(it) }
    FolderGroup.PAGES -> screenplay.names().map { it to PageNaming.folderOf(it) }
    FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> blockTypes?.types?.value.orEmpty().filter { it.kind == group.kind }.map { it.name to it.folder }
    FolderGroup.SOUNDS, FolderGroup.MUSIC -> knownResourcePaths().orEmpty().filter { ResourceKind.of(it) == group.resourceKind }.map { it to ResourceKind.audioFolderOf(it) }
}

/**
 * The game's own sounds and music the last resource scan found ([knownResourcePaths]).
 */
private fun CommandContext.knownAudioFiles(): AudioFiles {
    val paths = knownResourcePaths().orEmpty()
    return AudioFiles(sounds = paths.filter { ResourceKind.of(it) == ResourceKind.SOUND }, music = paths.filter { ResourceKind.of(it) == ResourceKind.MUSIC })
}

/**
 * Why there is no folder at [path] in [group] (the top of the group, an empty path, always exists), given its [folders].
 */
internal fun CommandContext.validateFolder(group: FolderGroup, path: String, folders: Set<String>? = null): Rejection? {
    if (group.kind != null && blockTypes == null) return Rejection(CommandProblem.NO_ACTOR_TYPES)
    if (path.isEmpty()) return null
    val existing = folders ?: foldersOf(group)
    return if (path in existing) null else SimilarNames.rejection(LookupProblem.FOLDER_MISSING, LookupProblem.FOLDER_MISSING_SIMILAR, path, existing, path)
}

/**
 * Why there can't be a new folder at [path] in [group].
 */
internal fun CommandContext.validateNewFolder(group: FolderGroup, path: String): Rejection? = when {
    group.kind != null && blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    path.isEmpty() -> Rejection(NameProblem.NAME_EMPTY)
    else -> folderNameRejection(group, path) ?: validateFreeFolder(group, path)
}

/**
 * Why [path], whose name can be a folder's in [group], can't be a new folder there.
 */
private fun CommandContext.validateFreeFolder(group: FolderGroup, path: String): Rejection? = when {
    group == FolderGroup.SCENES && SceneNaming.reservedFolderOf(path, isFolder = true) != null ->
        Rejection(CommandProblem.SCENE_FOLDER_RESERVED, SceneNaming.reservedFolderOf(path, isFolder = true).orEmpty())
    group.resourceKind != null && ResourceKind.isScenesFolder(path) -> Rejection(CommandProblem.AUDIO_FOLDER_RESERVED, path)
    existingFoldersOf(group).any { it.equals(path, ignoreCase = true) } -> Rejection(CommandProblem.FOLDER_EXISTS, path)
    else -> validateFolder(group, FolderPaths.parentOf(path))
}

/**
 * The folders a new folder of [group] can't be called like: its own, and for sounds and music, every real folder of
 * `files`, since one the group doesn't show (`music` among the sounds) is still there on disk.
 */
private fun CommandContext.existingFoldersOf(group: FolderGroup): Set<String> = if (group.resourceKind != null) audioFolders.folders() else foldersOf(group)

/**
 * Why [path] can't name a folder of [group]: a Scene's folders are part of its name, and so are a page's, and an audio
 * file's are part of its path.
 */
private fun folderNameRejection(group: FolderGroup, path: String): Rejection? = when (group) {
    FolderGroup.SCENES -> (SceneNaming.validate(path) ?: FolderPaths.validatePath(path))?.let(::Rejection)
    FolderGroup.PAGES -> validatePageName(path)
    FolderGroup.ACTOR_TYPES, FolderGroup.MANAGER_TYPES -> FolderPaths.validatePath(path)?.let(::Rejection)
    FolderGroup.SOUNDS, FolderGroup.MUSIC -> ResourceKind.validateAudioFolder(path)?.let(::Rejection)
}
