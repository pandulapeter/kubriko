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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.resources.validateExistingResource
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Why the file at [path] (from `composeResources`) can't be put in [folder] of [group], a group of virtual folders
 * (sprites' or fonts'), or null if it can.
 */
internal fun CommandContext.validatePutInVirtualFolder(group: FolderGroup, path: String, folder: String): Rejection? = when {
    ResourceKind.of(path) != group.resourceKind -> Rejection(LookupProblem.RESOURCE_MISSING, path)
    else -> validateExistingResource(path) ?: validateFolder(group, folder)
}

/**
 * Puts the file at [path] in [folder] of [group]'s virtual folders, whose undo is [inverse] of the folder it was in.
 */
@EditorMutation
internal suspend fun CommandContext.putInVirtualFolder(group: FolderGroup, path: String, folder: String, inverse: (String) -> Invocation<*>): Applied {
    val store = virtualFoldersOf(group)
    val name = ResourceKind.nameOf(path)
    val previousFolder = store.state.value.folderOf[name].orEmpty()
    try {
        withContext(ioDispatcher) { store.move(name, folder) }
    } catch (exception: IOException) {
        return Applied.failed(Rejection(CommandProblem.FILE_NOT_PUT_IN_FOLDER, path, exception.message ?: exception.toString()))
    }
    return Applied(inverse = inverse(previousFolder))
}
