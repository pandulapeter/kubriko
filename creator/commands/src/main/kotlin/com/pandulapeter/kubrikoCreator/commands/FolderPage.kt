/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal const val DEFAULT_LIST_PAGE_SIZE = 100

/**
 * One page of [items] in [folder] and its folders (each item's folder named by [folderOf], `""` for none), as the listing
 * tools return it: `total` counts the matching items, [itemsKey] holds up to [count] of them from [offset], `nextOffset`
 * is there while more follow, and `folders` lists the folders in [folder] out of [folders], every folder there is, empty
 * ones too.
 */
internal inline fun <T> folderPage(
    items: List<T>,
    folders: Set<String>,
    folder: String?,
    offset: Int,
    count: Int,
    itemsKey: String,
    folderOf: (T) -> String,
    itemJson: JsonObjectBuilder.(T) -> Unit,
): CommandOutcome {
    val trimmed = folder?.trim('/')?.takeIf { it.isNotEmpty() }
    when {
        offset < 0 || count < 1 -> return CommandOutcome.Rejected(Rejection(CommandProblem.PAGE_INVALID))
        trimmed != null && trimmed !in folders -> return CommandOutcome.Rejected(Rejection(CommandProblem.FOLDER_MISSING, trimmed))
    }
    val matching = items.filter { isInFolder(folderOf(it), trimmed) }
    val end = (offset.toLong() + count).coerceAtMost(matching.size.toLong()).toInt()
    return CommandOutcome.Success(
        buildJsonObject {
            put("total", matching.size)
            putJsonArray(itemsKey) {
                if (offset < end) matching.subList(offset, end).forEach { item -> addJsonObject { itemJson(item) } }
            }
            if (end < matching.size) put("nextOffset", end)
            putJsonArray("folders") { folders.filter { it != trimmed && isInFolder(it, trimmed) }.sorted().forEach { add(it) } }
        },
    )
}

internal fun isInFolder(path: String, folder: String?): Boolean = folder == null || path == folder || path.startsWith("$folder/")
