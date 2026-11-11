/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.USAGES
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.usagesDetails
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.withKotlinFilesHeld
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@OptIn(EditorMutation::class)
public object DeleteFolder : DeletingCommand<DeleteFolder.Input> {
    override val name: String = "delete_folder"
    override val description: String = "Deletes a folder of Actor types, Manager types, Scenes, pages, sprites, sounds, or music with everything in it, its folders too, each thing as its own " +
        "delete tool would, cleaning up what refers to it, all as one step. check_change lists what it deletes and changes."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun subject(input: Input): RelationSubject = RelationSubject.Folder(input.group, input.path)

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        input.keepsContents -> when {
            input.group.kind != null && context.blockTypes == null -> Rejection(CommandProblem.NO_ACTOR_TYPES)
            !input.group.isAudio && context.itemsOf(input.group).any { (_, folder) -> FolderPaths.isWithin(folder, input.path) } ->
                Rejection(CommandProblem.FOLDER_NOT_EMPTY, input.path)
            else -> null
        }
        else -> context.validateFolder(input.group, input.path) ?: if (input.path.isEmpty()) Rejection(CommandProblem.FOLDER_TOP) else childOutside(context, input)
    }

    /**
     * Why an Actor type in the folder can't go: a child of it outside the folder, which would be left without its parent.
     */
    private fun childOutside(context: CommandContext, input: Input): Rejection? {
        if (input.group != FolderGroup.ACTOR_TYPES) return null
        val types = context.requireBlockTypes.actorTypes.value
        val family = ActorTypeFamily(types)
        val inside = types.filter { FolderPaths.isWithin(it.folder, input.path) }.mapTo(HashSet()) { it.name }
        return inside.sorted().firstNotNullOfOrNull { parent ->
            family.childrenOf(parent).firstOrNull { it !in inside }?.let { child -> Rejection(CommandProblem.ACTOR_TYPE_HAS_CHILDREN, parent, child) }
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (input.keepsContents) {
            if (input.group.isAudio && withContext(context.ioDispatcher) { context.holdsAudioOnDisk(input.path) }) {
                return Applied.failed(Rejection(CommandProblem.FOLDER_NOT_EMPTY, input.path))
            }
            context.deleteFolder(input.group, input.path)
            return Applied(inverse = Invocation(CreateFolder, CreateFolder.Input(input.group, input.path)))
        }
        val folder = RelationSubject.Folder(input.group, input.path)
        val deletes = context.contentDeletesOf(folder)
        val folders = (context.foldersOf(input.group).filter { FolderPaths.isWithin(it, input.path) } + input.path)
            .distinct()
            .sortedByDescending { it.count { character -> character == '/' } }
            .map { Invocation(DeleteFolder, Input(input.group, it, keepsContents = true)) }
        val (usages, applied) = context.withKotlinFilesHeld {
            context.folderUsagesOf(folder, deletes, isAfterDeleting = true) to CompositeCommand.apply(context, CompositeCommand.Input(deletes + folders))
        }
        if (applied.failure != null) return applied
        val reportedByParts = applied.details?.get(USAGES)?.jsonArray?.mapTo(HashSet()) { it.jsonPrimitive.content }.orEmpty()
        val reported = usagesReported(usages, reportedByParts)
        val kept = applied.details.orEmpty() - USAGES
        return applied.copy(details = if (reported.isEmpty()) kept.takeIf { it.isNotEmpty() }?.let(::JsonObject) else JsonObject(kept + usagesDetails(reported)))
    }

    /**
     * @param keepsContents Whether it deletes only the folder and the folders in it, and only once nothing else is in
     *   them: the last parts of the delete, once their contents are gone, and what undoes a folder's creation. An
     *   audio folder's sounds and music are looked for on disk while it is applied, since the deletes before it in
     *   the same step leave the last resource scan behind. A
     *   folder already gone (one that held only types, which leaves none in memory) is deleted all the same, since
     *   its directory may still be on disk.
     */
    @Serializable
    public data class Input(
        @Description("What the folder holds.")
        @SerialName("group") val group: FolderGroup,
        @Description("The folder's path: folder names separated by slashes, like enemies/flying (for audio, from composeResources/files).")
        @SerialName("path") val path: String,
        @Transient val keepsContents: Boolean = false,
    )
}
