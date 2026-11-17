/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Writes a file a command made for the user ([RemoveCreatedFile] removed) back where none is: what redoing the making does.
 * A file there now, whoever wrote it, is left as it is, since it may be the user's.
 */
@OptIn(EditorMutation::class)
internal object RestoreCreatedFile : WriteCommand<RemoveCreatedFile.Input> {
    override val name = "restore_created_file"
    override val description = ""
    override val inputSerializer: KSerializer<RemoveCreatedFile.Input> = NotATool("RestoreCreatedFile")

    override fun target(context: CommandContext, input: RemoveCreatedFile.Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: RemoveCreatedFile.Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: RemoveCreatedFile.Input): Applied {
        withContext(context.ioDispatcher) {
            if (!File(context.project.rootDirectory, input.path).exists()) ReplaceFiles.writeNew(context, input.path, input.bytes)
        }
        return Applied(inverse = Invocation(RemoveCreatedFile, input))
    }
}
