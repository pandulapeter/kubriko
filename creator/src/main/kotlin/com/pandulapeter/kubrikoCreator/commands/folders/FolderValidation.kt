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
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.withParents
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.scenes.SceneNaming

/**
 * Every folder of [group], as paths (`enemies/flying`), parents included.
 */
internal fun CommandContext.foldersOf(group: FolderGroup): Set<String> = when (val kind = group.kind) {
    null -> (scenes.folders() + scenes.names().map(FolderPaths::parentOf).filter { it.isNotEmpty() }).withParents()
    else -> blockTypes?.folders?.value?.get(kind).orEmpty()
}

/**
 * The names of the types or Scenes of [group] and the folders they are in.
 */
internal fun CommandContext.itemsOf(group: FolderGroup): List<Pair<String, String>> = when (val kind = group.kind) {
    null -> scenes.names().map { it to FolderPaths.parentOf(it) }
    else -> blockTypes?.types?.value.orEmpty().filter { it.kind == kind }.map { it.name to it.folder }
}

/**
 * Why there is no folder at [path] in [group] (the top of the group, an empty path, always exists).
 */
internal fun CommandContext.validateFolder(group: FolderGroup, path: String): Rejection? = when {
    group.kind != null && blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    path.isNotEmpty() && path !in foldersOf(group) -> Rejection(CommandProblem.FOLDER_MISSING, path)
    else -> null
}

/**
 * Why there can't be a new folder at [path] in [group].
 */
internal fun CommandContext.validateNewFolder(group: FolderGroup, path: String): Rejection? = when {
    group.kind != null && blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
    path.isEmpty() || !FolderPaths.isValid(path) -> Rejection(NameProblem.FOLDER_INVALID)
    // A Scene's folders are part of its name.
    group == FolderGroup.SCENES && !SceneNaming.isValid(path) -> Rejection(CommandProblem.SCENE_NAME_INVALID)
    group == FolderGroup.SCENES && SceneNaming.reservedFolderOf(path, isFolder = true) != null ->
        Rejection(CommandProblem.SCENE_FOLDER_RESERVED, SceneNaming.reservedFolderOf(path, isFolder = true).orEmpty())
    foldersOf(group).any { it.equals(path, ignoreCase = true) } -> Rejection(CommandProblem.FOLDER_EXISTS, path)
    else -> validateFolder(group, FolderPaths.parentOf(path))
}

/**
 * Whether anything (a type, a Scene, or a folder) is in the folder at [path] of [group].
 */
internal fun CommandContext.isFolderEmpty(group: FolderGroup, path: String) = itemsOf(group).none { (_, folder) -> FolderPaths.isWithin(folder, path) } &&
    foldersOf(group).none { it != path && FolderPaths.isWithin(it, path) }
