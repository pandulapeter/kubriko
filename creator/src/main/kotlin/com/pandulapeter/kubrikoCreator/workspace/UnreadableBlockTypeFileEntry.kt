/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeFileProblem
import com.pandulapeter.kubrikoCreator.actors.store.UnreadableBlockTypeFile
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry

/**
 * The console's report of why this file was left out.
 */
internal fun UnreadableBlockTypeFile.toConsoleEntry() = when (val problem = problem) {
    is BlockTypeFileProblem.Unparsable -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_UNREADABLE, isError = true, arguments = listOf(path, problem.message))
    is BlockTypeFileProblem.NameMismatch -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_NAME_MISMATCH, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.InvalidName -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_NAME_INVALID, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.ReservedName -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_NAME_RESERVED, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.InvalidPropertyName -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_PROPERTY_NAME_INVALID, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.DuplicateName -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FILE_NAME_DUPLICATE, isError = true, arguments = listOf(path, problem.otherPaths.joinToString()))
}
