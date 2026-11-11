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
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.folders.DEFAULT_LIST_PAGE_SIZE
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.folderPage
import com.pandulapeter.kubrikoCreator.commands.folders.itemsOf
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put

public object ListResources : ReadCommand<ListResources.Input> {
    override val name: String = "list_resources"
    override val description: String = "Lists the sprites, sounds, or music (by path from composeResources) a page at a time with the folder each is in, " +
        "and how a sprite is cut into frames (set_sprite_sheet), as list_actor_types does."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        if (input.group.resourceKind == null) return CommandOutcome.Rejected(Rejection(CommandProblem.NOT_A_RESOURCE_GROUP))
        return context.folderPage(
            group = input.group,
            items = context.itemsOf(input.group),
            folder = input.folder,
            offset = input.offset,
            count = input.count,
            itemsKey = "resources",
            folderOf = { (_, folder) -> folder },
        ) { (path, folder) ->
            put("path", path)
            put("folder", folder)
            if (input.group == FolderGroup.SPRITES) {
                context.spriteFolders.state.value.sheets[ResourceKind.nameOf(path)]?.let { put("sheet", Json.encodeToJsonElement(SpriteSheet.serializer(), it)) }
            }
        }
    }

    @Serializable
    public data class Input(
        @Description("sprites, sounds, or music.")
        @SerialName("group") val group: FolderGroup,
        @Description("Only this folder and its folders. Left out, all.")
        @SerialName("folder") val folder: String? = null,
        @Description("How many to skip: the previous page's nextOffset.")
        @SerialName("offset") val offset: Int = 0,
        @Description("At most how many to return.")
        @SerialName("count") val count: Int = DEFAULT_LIST_PAGE_SIZE,
    )
}
