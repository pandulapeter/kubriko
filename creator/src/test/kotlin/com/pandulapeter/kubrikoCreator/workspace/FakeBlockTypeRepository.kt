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
import com.pandulapeter.kubrikoCreator.actors.LoadedBlockTypes
import com.pandulapeter.kubrikoCreator.actors.RegeneratedCode
import com.pandulapeter.kubrikoCreator.actors.UnreadableBlockTypeFile
import java.io.IOException

internal class FakeBlockTypeRepository(
    initial: List<BlockTypeDefinition> = emptyList(),
) : BlockTypeRepository {
    val stored = initial.associateBy { it.name }.toMutableMap()
    val writes = mutableListOf<String>()
    var isFailing = false

    var unreadableFiles = emptyList<UnreadableBlockTypeFile>()

    var loadFailure: Exception? = null
    var regenerateFailure: Exception? = null

    override fun load() = loadFailure?.let { throw it } ?: LoadedBlockTypes(stored.values.sortedBy { it.name }, unreadableFiles)

    var changedByHand = emptySet<String>()

    override fun save(definition: BlockTypeDefinition) {
        if (isFailing) throw IOException("Disk full.")
        writes += "save ${definition.name}"
        stored[definition.name] = definition
    }

    var removedLeftovers = emptyList<String>()
    var leftoversChangedByHand = emptySet<String>()

    override fun regenerate(definitions: List<BlockTypeDefinition>, overwriting: Set<String>): RegeneratedCode {
        if (isFailing) throw IOException("Disk full.")
        regenerateFailure?.let { throw it }
        writes += if (overwriting.isEmpty()) "regenerate" else "regenerate overwriting ${overwriting.sorted().joinToString()}"
        return RegeneratedCode(changedByHand - overwriting, removedLeftovers, leftoversChangedByHand)
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

    override fun delete(name: String) {
        if (isFailing) throw IOException("Disk full.")
        writes += "delete $name"
        stored.remove(name)
    }
}
