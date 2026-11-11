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
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.ReplaceScenes
import com.pandulapeter.kubrikoCreator.commands.ReplaceTypes
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.commands.screenplay.withMentionsFollowed
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.HandWrittenKotlin
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.resourceNaming
import com.pandulapeter.kubrikoCreator.context.resourceUsagesOf
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
import java.io.IOException

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
        val restoring = listOfNotNull(movingBack, context.followInTypes(input.path, input.newPath), context.followInScenes(input.path, input.newPath))
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
     * Makes the types that use [path] use [newPath].
     *
     * @return What puts back the types it changed as they were, or null if it changed none.
     */
    private fun CommandContext.followInTypes(path: String, newPath: String): Invocation<*>? {
        val blockTypes = blockTypes ?: return null
        val previous = blockTypes.types.value.mapNotNull { definition ->
            definition.withResourceRenamed(path, newPath).takeIf { it != definition }?.let { renamed ->
                blockTypes.put(renamed)
                definition
            }
        }
        return previous.takeIf { it.isNotEmpty() }?.let { Invocation(ReplaceTypes, ReplaceTypes.Input(it)) }
    }

    /**
     * Makes the Scenes playing the music at [path] play [newPath], all of them or none, saying in the console which still
     * play it when they can't be written.
     *
     * @return What puts back the Scenes it changed byte for byte, or null if it changed none.
     */
    private suspend fun CommandContext.followInScenes(path: String, newPath: String): Invocation<*>? {
        if (ResourceKind.of(path) != ResourceKind.MUSIC) return null
        val previous = withContext(ioDispatcher) {
            val contents = scenesWithMusicReplaced(scenes.scanNames(), path, newPath)
            val texts = contents.keys.associateWith { checkNotNull(scenes.read(it)) }
            try {
                scenes.writeAllTexts(contents)
                texts
            } catch (exception: IOException) {
                val reason = exception.message ?: exception.toString()
                report(ConsoleEntry.Status(CommandProblem.RESOURCE_MUSIC_NOT_FOLLOWED, isError = true, arguments = listOf(contents.keys.joinToString(), path, reason)))
                emptyMap()
            }
        }
        return previous.takeIf { it.isNotEmpty() }?.let { Invocation(ReplaceScenes, ReplaceScenes.Input(it)) }
    }

    /**
     * Says in the console where Kotlin still names [path] once it is [newPath]: the Kotlin Blocks and Kotlin UI of the
     * types, and hand-written files, none of which Kubriko Creator rewrites.
     */
    private suspend fun CommandContext.reportNamedInCode(path: String, newPath: String) {
        val subject = RelationSubject.Resource(path)
        val inTypes = resourceUsagesOf(subject, blockTypes?.types?.value.orEmpty(), emptyMap()).filter { it.kind == RelationKind.NAMES_IN_KOTLIN }.map { it.place.toString() }
        val isNaming = resourceNaming(subject)
        val inFiles = withContext(ioDispatcher) { HandWrittenKotlin.read(project.rootDirectory).filterValues(isNaming).keys.sorted() }
        (inTypes + inFiles).forEach { place -> report(ConsoleEntry.Status(CommandProblem.RESOURCE_NAMED_IN_CODE, isError = true, arguments = listOf(place, path, newPath))) }
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
