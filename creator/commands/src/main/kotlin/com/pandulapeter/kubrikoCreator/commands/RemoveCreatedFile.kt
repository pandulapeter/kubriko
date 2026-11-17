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
import java.io.IOException

/**
 * Deletes a file a command made for the user to go on with (a hand-written Composable's), as long as it still holds what
 * it was made with: what undoes making it, which never takes what the user wrote since. A changed file is left as it is.
 */
@OptIn(EditorMutation::class)
internal object RemoveCreatedFile : WriteCommand<RemoveCreatedFile.Input> {
    override val name = "remove_created_file"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("RemoveCreatedFile")

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val isUnchanged = withContext(context.ioDispatcher) {
            try {
                File(context.project.rootDirectory, input.path).takeIf { it.isFile }?.readBytes()?.contentEquals(input.bytes) == true
            } catch (_: IOException) {
                false
            }
        }
        if (!isUnchanged) return Applied(inverse = Invocation(RemoveCreatedFile, input))
        return ReplaceFiles.apply(context, ReplaceFiles.Input(mapOf(input.path to null)))
    }

    /**
     * @param path The file's path from the project's root.
     * @param bytes What it was made with.
     */
    class Input(
        val path: String,
        val bytes: ByteArray,
    )
}
