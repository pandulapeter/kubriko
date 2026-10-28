/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import java.io.IOException

internal class FakeBlockTypeRepository(
    initial: List<BlockTypeDefinition> = emptyList(),
) : BlockTypeRepository {
    val stored = initial.associateBy { it.name }.toMutableMap()
    val writes = mutableListOf<String>()
    var isFailing = false

    override fun load() = stored.values.sortedBy { it.name }

    var changedByHand = emptySet<String>()

    override fun save(definition: BlockTypeDefinition): Set<String> {
        if (isFailing) throw IOException("Disk full.")
        writes += "save ${definition.name}"
        stored[definition.name] = definition
        return changedByHand
    }

    override fun regenerateAll(overwriting: Set<String>): Set<String> {
        if (isFailing) throw IOException("Disk full.")
        writes += if (overwriting.isEmpty()) "regenerate" else "regenerate overwriting ${overwriting.sorted().joinToString()}"
        return changedByHand - overwriting
    }

    val folders = mutableMapOf<BlockTypeKind, MutableSet<String>>()

    override fun folders(kind: BlockTypeKind): Set<String> = folders[kind].orEmpty()

    override fun createFolder(kind: BlockTypeKind, path: String) {
        writes += "create folder $path"
        folders.getOrPut(kind) { mutableSetOf() } += path
    }

    override fun deleteFolder(kind: BlockTypeKind, path: String) {
        writes += "delete folder $path"
        folders[kind]?.removeAll { it == path || it.startsWith("$path/") }
    }

    override fun delete(name: String): Set<String> {
        if (isFailing) throw IOException("Disk full.")
        writes += "delete $name"
        stored.remove(name)
        return changedByHand
    }
}
