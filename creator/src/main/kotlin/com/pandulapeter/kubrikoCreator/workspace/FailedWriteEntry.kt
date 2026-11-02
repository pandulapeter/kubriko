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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_block_type_not_saved
import kubriko.creator.generated.resources.status_generated_code_not_written
import kubriko.creator.generated.resources.status_type_folder_not_saved

/**
 * The console's report of what couldn't be written, and why.
 */
internal fun FailedWrite.toConsoleEntry() = when (subject) {
    is FailedWrite.Subject.Type -> ConsoleEntry.Status(Res.string.status_block_type_not_saved, isError = true, arguments = listOf(subject.name, reason))
    is FailedWrite.Subject.Folder -> ConsoleEntry.Status(Res.string.status_type_folder_not_saved, isError = true, arguments = listOf(subject.path, reason))
    FailedWrite.Subject.GeneratedCode -> ConsoleEntry.Status(Res.string.status_generated_code_not_written, isError = true, arguments = listOf(reason))
}
