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
import com.pandulapeter.kubrikoCreator.actors.store.FailedWrite
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry

/**
 * The console's report of what couldn't be written, and why.
 */
internal fun FailedWrite.toConsoleEntry() = when (subject) {
    is FailedWrite.Subject.Type -> ConsoleEntry.Status(WorkspaceStatus.BLOCK_TYPE_NOT_SAVED, isError = true, arguments = listOf(subject.name, reason))
    is FailedWrite.Subject.Folder -> ConsoleEntry.Status(WorkspaceStatus.TYPE_FOLDER_NOT_SAVED, isError = true, arguments = listOf(subject.path, reason))
    FailedWrite.Subject.GeneratedCode -> ConsoleEntry.Status(WorkspaceStatus.GENERATED_CODE_NOT_WRITTEN, isError = true, arguments = listOf(reason))
}
