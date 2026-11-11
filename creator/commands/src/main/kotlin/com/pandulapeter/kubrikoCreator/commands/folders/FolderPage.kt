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

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal const val DEFAULT_LIST_PAGE_SIZE = 100

/**
 * One page of [items] of [group] in [folder] and its folders (each item's folder named by [folderOf], `""` for none), as the
 * listing tools return it: `total` counts the matching items, [itemsKey] holds up to [count] of them from [offset],
 * `nextOffset` is there while more follow, and `folders` lists the group's folders in [folder], empty ones too.
 */
internal fun <T> CommandContext.folderPage(
    group: FolderGroup,
    items: List<T>,
    folder: String?,
    offset: Int,
    count: Int,
    itemsKey: String,
    folderOf: (T) -> String,
    itemJson: JsonObjectBuilder.(T) -> Unit,
): CommandOutcome {
    val path = folder?.trim('/').orEmpty()
    val folders = foldersOf(group)
    val rejection = if (offset < 0 || count < 1) Rejection(CommandProblem.PAGE_INVALID) else validateFolder(group, path, folders)
    if (rejection != null) return CommandOutcome.Rejected(rejection)
    fun isIn(itemFolder: String) = path.isEmpty() || FolderPaths.isWithin(itemFolder, path)
    val matching = items.filter { isIn(folderOf(it)) }
    val end = (offset.toLong() + count).coerceAtMost(matching.size.toLong()).toInt()
    return CommandOutcome.Success(
        buildJsonObject {
            put("total", matching.size)
            putJsonArray(itemsKey) {
                if (offset < end) matching.subList(offset, end).forEach { item -> addJsonObject { itemJson(item) } }
            }
            if (end < matching.size) put("nextOffset", end)
            putJsonArray("folders") { folders.filter { it != path && isIn(it) }.sorted().forEach { add(it) } }
        },
    )
}
