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
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_folder_exists
import kubriko.creator.generated.resources.error_folder_missing
import kubriko.creator.generated.resources.error_folder_name_invalid

internal fun CommandContext.foldersOf(group: FolderGroup) = blockTypes?.folders?.value?.get(group.kind).orEmpty()

/**
 * Why there is no folder at [path] in [group] (the top of the group, an empty path, always exists).
 */
internal fun CommandContext.validateFolder(group: FolderGroup, path: String): Rejection? = when {
    blockTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    path.isNotEmpty() && path !in foldersOf(group) -> Rejection(Res.string.error_folder_missing, path)
    else -> null
}

/**
 * Why there can't be a new folder at [path] in [group].
 */
internal fun CommandContext.validateNewFolder(group: FolderGroup, path: String): Rejection? = when {
    blockTypes == null -> Rejection(Res.string.error_command_no_actor_types)
    path.isEmpty() || !FolderPaths.isValid(path) -> Rejection(Res.string.error_folder_name_invalid)
    foldersOf(group).any { it.equals(path, ignoreCase = true) } -> Rejection(Res.string.error_folder_exists, path)
    else -> validateFolder(group, FolderPaths.parentOf(path))
}
