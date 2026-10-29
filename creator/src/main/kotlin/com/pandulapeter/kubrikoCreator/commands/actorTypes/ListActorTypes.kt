/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_list_actor_types
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_page_invalid
import kubriko.creator.generated.resources.error_folder_missing

internal object ListActorTypes : ReadCommand<ListActorTypes.Input> {
    override val name = "list_actor_types"
    override val description = "Lists the block-built Actor types of the project a page at a time: the name of each one, its folder and parent when it has them, " +
        "and the Events it reacts to. total counts the matching types, and nextOffset, when there are more, is the offset of the next page. " +
        "folders lists every Actor type folder (in the folder asked for), empty ones too."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_list_actor_types

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val actorTypes = context.blockTypes ?: return CommandOutcome.Rejected(Rejection(Res.string.error_command_no_actor_types))
        val folders = actorTypes.folders.value[BlockTypeKind.ACTOR].orEmpty()
        val folder = input.folder?.trim('/')?.takeIf { it.isNotEmpty() }
        when {
            input.offset < 0 || input.count < 1 -> return CommandOutcome.Rejected(Rejection(Res.string.error_command_page_invalid))
            folder != null && folder !in folders -> return CommandOutcome.Rejected(Rejection(Res.string.error_folder_missing, folder))
        }
        fun isIn(path: String) = folder == null || path == folder || path.startsWith("$folder/")
        val matching = actorTypes.actorTypes.value.filter { isIn(it.folder) }
        val end = (input.offset.toLong() + input.count).coerceAtMost(matching.size.toLong()).toInt()
        val changedByHand = actorTypes.changedByHand.value
        return CommandOutcome.Success(
            buildJsonObject {
                put("total", matching.size)
                putJsonArray("actorTypes") {
                    if (input.offset < end) {
                        matching.subList(input.offset, end).forEach { definition ->
                            addJsonObject {
                                put("name", definition.name)
                                if (definition.folder.isNotEmpty()) put("folder", definition.folder)
                                definition.parent?.let { put("parent", it) }
                                putJsonArray("events") { definition.events.forEach { add(it.type.serialName) } }
                                put("isChangedByHand", definition.name in changedByHand)
                            }
                        }
                    }
                }
                if (end < matching.size) put("nextOffset", end)
                putJsonArray("folders") { folders.filter { it != folder && isIn(it) }.sorted().forEach { add(it) } }
            },
        )
    }

    @Serializable
    data class Input(
        @Description("Only the Actor types in this folder and its folders, as a path like enemies/flying. Left out, every one.")
        @SerialName("folder") val folder: String? = null,
        @Description("How many matching Actor types to skip: the nextOffset of the previous page.")
        @SerialName("offset") val offset: Int = 0,
        @Description("At most how many Actor types to return.")
        @SerialName("count") val count: Int = DEFAULT_PAGE_SIZE,
    )

    const val DEFAULT_PAGE_SIZE = 100
}
