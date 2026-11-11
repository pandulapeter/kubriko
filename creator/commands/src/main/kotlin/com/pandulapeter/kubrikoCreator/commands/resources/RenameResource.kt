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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@OptIn(EditorMutation::class)
public object RenameResource : WriteCommand<RenameResource.Input> {
    override val name: String = "rename_resource"
    override val description: String = "Renames a sprite, sound, or music file (moving audio between folders of files too). What uses it follows, " +
        "but hand-written Kotlin."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateExistingResource(input.path)
        ?: Rejection(CommandProblem.RESOURCE_UNCHANGED, input.path).takeIf { input.newPath == input.path }
        ?: validateRestoring(context, input)
        ?: context.validateNewResource(input.newPath, except = input.path)

    /**
     * Checks only that the file stays what it is: undoing a rename comes right after it, before the resource scan that
     * knows the new name, brings back a name that may break today's rules, and applying checks the files on disk anyway.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? {
        val extension = input.path.substringAfterLast('.', "")
        return when {
            ResourceKind.of(input.path) == null -> Rejection(LookupProblem.RESOURCE_MISSING, input.path)
            ResourceKind.of(input.newPath) != ResourceKind.of(input.path) || input.newPath.substringAfterLast('.', "") != extension -> Rejection(CommandProblem.RESOURCE_KIND_CHANGED, extension)
            else -> null
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val files = withContext(context.ioDispatcher) { context.resourceFilesOnDisk() }
        if (files.isTaken(input.newPath, except = input.path)) return Applied.failed(Rejection(CommandProblem.RESOURCE_TAKEN, input.newPath))
        val moves = withContext(context.ioDispatcher) { context.moveResource(files.at(input.path), input.path, input.newPath) }
            ?: return Applied.failed(Rejection(CommandProblem.RESOURCE_NOT_MOVED, input.path))
        if (moves.isEmpty()) return Applied.failed(Rejection(LookupProblem.RESOURCE_MISSING, input.path))
        context.onFilesChanged()
        // Restoring, since the resource scan that knows the new name may come after the undo.
        val movingBack = Invocation(RenameResource, Input(input.newPath, input.path, movesOnly = true), isRestoring = true)
        if (input.movesOnly) return Applied(inverse = movingBack)
        context.reportNamedInCode(input.path, input.newPath)
        val renames = mapOf(input.path to input.newPath)
        val restoring = listOfNotNull(movingBack, context.followInTypes(renames), context.followInScenes(renames))
        return context.withMentionsFollowed(moves, Applied(inverse = restoring.singleOrNull() ?: Invocation(CompositeCommand, CompositeCommand.Input(restoring))))
    }

    /**
     * Moves [files] (each at [path] from its `composeResources` folder) to [newPath], moving back those moved already
     * when one can't be.
     *
     * @return How the files moved, by their paths from the project's root, or null if one couldn't be moved.
     */
    private fun CommandContext.moveResource(files: List<File>, path: String, newPath: String): List<PathMove>? {
        val moved = ArrayList<Pair<File, File>>()
        files.forEach { file ->
            val target = movedResourceFile(file, path, newPath)
            if (!file.moveWithoutReplacing(target)) {
                moveBack(moved)
                return null
            }
            scenes.ownWrites.record(file)
            scenes.ownWrites.record(target)
            moved += file to target
        }
        return moved.map { (from, to) -> PathMove(projectPathOf(from), projectPathOf(to)) }
    }

    /**
     * @param movesOnly Whether it moves only the files, leaving what uses them: how undo and redo move them, while the
     *   steps beside it put back exactly what the rename changed.
     */
    @Serializable
    public data class Input(
        @Description("Its path from composeResources, like drawable/ball.png.")
        @SerialName("path") val path: String,
        @Description("Its new path, with the same extension.")
        @SerialName("newPath") val newPath: String,
        @Transient val movesOnly: Boolean = false,
    )
}
