/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.DEFAULT_LIST_PAGE_SIZE
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.serialName
import com.pandulapeter.kubrikoCreator.commands.folderPage
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object ListManagerTypes : ReadCommand<ListManagerTypes.Input> {
    override val name: String = "list_manager_types"
    override val description: String = "Lists the block-built Manager types a page at a time, as list_actor_types does, " +
        "each with its Properties (name and kind), which Blocks of every type can use."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val types = context.blockTypes ?: return CommandOutcome.Rejected(Rejection(CommandProblem.NO_ACTOR_TYPES))
        val changedByHand = types.changedByHand.value
        return folderPage(
            items = types.managerTypes.value,
            folders = types.folders.value[BlockTypeKind.MANAGER].orEmpty(),
            folder = input.folder,
            offset = input.offset,
            count = input.count,
            itemsKey = "managerTypes",
            folderOf = { it.folder },
        ) { definition ->
            put("name", definition.name)
            if (definition.folder.isNotEmpty()) put("folder", definition.folder)
            putJsonArray("properties") {
                definition.properties.forEach { property ->
                    addJsonObject {
                        put("name", property.name)
                        put("kind", PropertyKind.of(property).serialName)
                    }
                }
            }
            putJsonArray("events") { definition.events.forEach { add(it.type.serialName) } }
            put("isChangedByHand", definition.name in changedByHand)
        }
    }

    @Serializable
    public data class Input(
        @Description("Only this folder and its folders, as a path like ui/menus. Left out, all.")
        @SerialName("folder") val folder: String? = null,
        @Description("How many to skip: the previous page's nextOffset.")
        @SerialName("offset") val offset: Int = 0,
        @Description("At most how many to return.")
        @SerialName("count") val count: Int = DEFAULT_LIST_PAGE_SIZE,
    )
}
