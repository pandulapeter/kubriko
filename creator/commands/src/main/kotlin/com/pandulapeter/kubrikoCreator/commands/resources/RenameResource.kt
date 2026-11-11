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
import com.pandulapeter.kubrikoCreator.actors.withResourceRenamed
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.HandWrittenKotlin
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.resourceNaming
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.PathMove
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException

@OptIn(EditorMutation::class)
public object RenameResource : WriteCommand<RenameResource.Input> {
    override val name: String = "rename_resource"
    override val description: String = "Renames a sprite, sound, or music file (moving audio between folders of files too). What uses it follows, " +
        "but hand-written Kotlin."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateExistingResource(input.path)
        ?: validateRestoring(context, input)
        ?: context.validateNewResource(input.newPath, except = input.path)

    /**
     * Checks only that the file stays what it is: undoing a rename comes right after it, before the resource scan that
     * knows the new name, and applying checks the files on disk anyway.
     */
    override fun validateRestoring(context: CommandContext, input: Input): Rejection? {
        val extension = input.path.substringAfterLast('.', "")
        return when {
            ResourceKind.of(input.path) == null -> Rejection(LookupProblem.RESOURCE_MISSING, input.path)
            ResourceKind.of(input.newPath) != ResourceKind.of(input.path) || input.newPath.substringAfterLast('.', "") != extension -> Rejection(CommandProblem.RESOURCE_KIND_CHANGED, extension)
            else -> ResourceKind.validate(input.newPath)?.let(::Rejection)
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val moves = withContext(context.ioDispatcher) { context.moveResource(input.path, input.newPath) } ?: return Applied.failed(Rejection(CommandProblem.RESOURCE_NOT_MOVED, input.path))
        if (moves.isEmpty()) return Applied.failed(Rejection(LookupProblem.RESOURCE_MISSING, input.path))
        context.onFilesChanged()
        context.followRename(input.path, input.newPath)
        // Restoring, since the resource scan that knows the new name may come after the undo, inside withMentionsFollowed's step too.
        return context.withMentionsFollowed(moves, Applied(inverse = Invocation(RenameResource, Input(input.newPath, input.path), isRestoring = true)))
    }

    /**
     * Moves every file at [path] (from `composeResources`, one per source set that has one) to [newPath], moving back
     * those moved already when one can't be.
     *
     * @return How the files moved, by their paths from the project's root; none if there was none to move, or null if
     *   one couldn't be moved or a file the game would mix up with [newPath] is there.
     */
    private fun CommandContext.moveResource(path: String, newPath: String): List<PathMove>? {
        if (isResourceTakenOnDisk(newPath, except = path)) return null
        val moved = ArrayList<Pair<File, File>>()
        resourceFilesAt(path).forEach { file ->
            val target = movedResourceFile(file, path, newPath)
            if (!file.moveWithoutReplacing(target)) {
                moved.asReversed().forEach { (from, to) -> to.moveWithoutReplacing(from) }
                return null
            }
            scenes.ownWrites.record(file)
            scenes.ownWrites.record(target)
            moved += file to target
        }
        return moved.map { (from, to) -> PathMove(projectPathOf(from), projectPathOf(to)) }
    }

    /**
     * Makes the types and Scenes that use [path] use [newPath], and says in the console which hand-written Kotlin files
     * still name it, and which Scenes still play it when they can't be written (all of them or none are).
     */
    private suspend fun CommandContext.followRename(path: String, newPath: String) {
        blockTypes?.let { blockTypes ->
            blockTypes.types.value.forEach { definition ->
                val renamed = definition.withResourceRenamed(path, newPath)
                if (renamed != definition) blockTypes.put(renamed)
            }
        }
        val isNaming = resourceNaming(RelationSubject.Resource(path))
        val namingFiles = withContext(ioDispatcher) {
            val sceneContents = scenesWithMusicReplaced(scenes.scanNames(), path, newPath)
            try {
                scenes.writeAllTexts(sceneContents)
            } catch (exception: IOException) {
                val reason = exception.message ?: exception.toString()
                report(ConsoleEntry.Status(CommandProblem.RESOURCE_MUSIC_NOT_FOLLOWED, isError = true, arguments = listOf(sceneContents.keys.joinToString(), path, reason)))
            }
            HandWrittenKotlin.read(project.rootDirectory).filterValues(isNaming).keys.sorted()
        }
        namingFiles.forEach { file -> report(ConsoleEntry.Status(CommandProblem.RESOURCE_NAMED_IN_CODE, isError = true, arguments = listOf(file, path, newPath))) }
    }

    @Serializable
    public data class Input(
        @Description("Its path from composeResources, like drawable/ball.png.")
        @SerialName("path") val path: String,
        @Description("Its new path, with the same extension.")
        @SerialName("newPath") val newPath: String,
    )
}
