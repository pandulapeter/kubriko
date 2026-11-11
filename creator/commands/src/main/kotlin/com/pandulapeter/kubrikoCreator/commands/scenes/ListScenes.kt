/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.folders.DEFAULT_LIST_PAGE_SIZE
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.folderPage
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put

public object ListScenes : ReadCommand<ListScenes.Input> {
    override val name: String = "list_scenes"
    override val description: String = "Lists the Scenes (named by path, like levels/1-1) a page at a time, as list_actor_types does, " +
        "each with how many Actors it has, or that its file isn't a valid Scene."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome = withContext(context.ioDispatcher) {
        context.folderPage(
            group = FolderGroup.SCENES,
            items = context.scenes.names(),
            folder = input.folder,
            offset = input.offset,
            count = input.count,
            itemsKey = "scenes",
            folderOf = FolderPaths::parentOf,
        ) { name ->
            put("name", name)
            if (context.scenes.isValid(name)) put("actorCount", context.readScene(name).size) else put("isValid", false)
        }
    }

    @Serializable
    public data class Input(
        @Description("Only this folder and its folders, as a path like levels/forest. Left out, all.")
        @SerialName("folder") val folder: String? = null,
        @Description("How many to skip: the previous page's nextOffset.")
        @SerialName("offset") val offset: Int = 0,
        @Description("At most how many to return.")
        @SerialName("count") val count: Int = DEFAULT_LIST_PAGE_SIZE,
    )
}
