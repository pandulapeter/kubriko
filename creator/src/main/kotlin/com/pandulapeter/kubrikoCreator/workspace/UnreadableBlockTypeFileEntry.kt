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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeFileProblem
import com.pandulapeter.kubrikoCreator.actors.UnreadableBlockTypeFile
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_type_file_name_duplicate
import kubriko.creator.generated.resources.status_type_file_name_invalid
import kubriko.creator.generated.resources.status_type_file_name_mismatch
import kubriko.creator.generated.resources.status_type_file_name_reserved
import kubriko.creator.generated.resources.status_type_file_unreadable

/**
 * The console's report of why this file was left out.
 */
internal fun UnreadableBlockTypeFile.toConsoleEntry() = when (problem) {
    is BlockTypeFileProblem.Unparsable -> ConsoleEntry.Status(Res.string.status_type_file_unreadable, isError = true, arguments = listOf(path, problem.message))
    is BlockTypeFileProblem.NameMismatch -> ConsoleEntry.Status(Res.string.status_type_file_name_mismatch, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.InvalidName -> ConsoleEntry.Status(Res.string.status_type_file_name_invalid, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.ReservedName -> ConsoleEntry.Status(Res.string.status_type_file_name_reserved, isError = true, arguments = listOf(path, problem.name))
    is BlockTypeFileProblem.DuplicateName -> ConsoleEntry.Status(Res.string.status_type_file_name_duplicate, isError = true, arguments = listOf(path, problem.otherPaths.joinToString()))
}
