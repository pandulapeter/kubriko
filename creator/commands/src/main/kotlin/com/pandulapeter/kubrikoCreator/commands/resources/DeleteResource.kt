/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceFiles
import com.pandulapeter.kubrikoCreator.commands.deleteWithUsages
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.IOException

@OptIn(EditorMutation::class)
public object DeleteResource : DeletingCommand<DeleteResource.Input> {
    override val name: String = "delete_resource"
    override val description: String = "Deletes a sprite, sound, or music file. What uses it is cleaned up and pages showing it keep a copy; check_change lists what."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun subject(input: Input): RelationSubject = RelationSubject.Resource(input.path)

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateExistingResource(input.path)

    /**
     * Checks nothing: undoing an addition comes right after it, before the resource scan that knows the new file, and
     * applying finds the files on disk anyway.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (!input.keepsUsages) return context.deleteWithUsages(subject(input), Invocation(DeleteResource, input.copy(keepsUsages = true)))
        val paths = withContext(context.ioDispatcher) { context.resourceFilesAt(input.path).map(context::projectPathOf) }
        if (paths.isEmpty()) return Applied.failed(Rejection(LookupProblem.RESOURCE_MISSING, input.path))
        return try {
            ReplaceFiles.apply(context, ReplaceFiles.Input(paths.associateWith { null }))
        } catch (_: IOException) {
            Applied.failed(Rejection(CommandProblem.RESOURCE_NOT_DELETED, input.path))
        }
    }

    /**
     * @param keepsUsages Whether it deletes only the file itself (in every source set that has one), leaving what refers
     *   to it: the last part of the delete, after [deleteWithUsages] cleaned that up, and what undoes its addition.
     */
    @Serializable
    public data class Input(
        @Description("Its path from composeResources, like drawable/ball.png.")
        @SerialName("path") val path: String,
        @Transient val keepsUsages: Boolean = false,
    )
}
