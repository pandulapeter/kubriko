/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.store.LoadedBlockTypes
import com.pandulapeter.kubrikoCreator.actors.store.RegeneratedCode
import com.pandulapeter.kubrikoCreator.actors.store.UnreadableBlockTypeFile
import java.io.IOException

internal class FakeBlockTypeRepository(
    initial: List<BlockTypeDefinition> = emptyList(),
) : BlockTypeRepository {
    val stored = initial.associateBy { it.name }.toMutableMap()
    val writes = mutableListOf<String>()
    var isFailing = false

    /**
     * The types whose save or delete throws, and the folders whose creation or deletion does.
     */
    val failingNames = mutableSetOf<String>()

    /**
     * The types whose save throws, while their delete succeeds.
     */
    val failingSaveNames = mutableSetOf<String>()

    /**
     * Called at the end of every [load], after the files were read, as if something happened meanwhile.
     */
    var onLoad: () -> Unit = {}
    var loadCount = 0

    var unreadableFiles = emptyList<UnreadableBlockTypeFile>()

    var loadFailure: Exception? = null
    var regenerateFailure: Exception? = null

    override fun load(): LoadedBlockTypes {
        loadCount++
        val loaded = loadFailure?.let { throw it } ?: LoadedBlockTypes(stored.values.sortedBy { it.name }, unreadableFiles)
        onLoad()
        return loaded
    }

    var changedByHand = emptySet<String>()

    override fun save(definition: BlockTypeDefinition, isReplacing: Boolean) {
        if (isFailing) throw IOException("Disk full.")
        if (definition.name in failingNames || definition.name in failingSaveNames) throw IOException("Locked.")
        writes += if (isReplacing) "replace ${definition.name}" else "save ${definition.name}"
        stored[definition.name]?.takeIf { it.folder != definition.folder }?.let(::keepFolderOf)
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

    override fun folders(kind: BlockTypeKind): Set<String> = (folders[kind].orEmpty() + stored.values.filter { it.kind == kind }.map { it.folder })
        .flatMapTo(sortedSetOf(), ::withParents)

    override fun createFolder(kind: BlockTypeKind, path: String) {
        if (path in failingNames) throw IOException("Locked.")
        writes += "create folder $path"
        folders.getOrPut(kind) { mutableSetOf() } += path
    }

    override fun deleteFolder(kind: BlockTypeKind, path: String) {
        if (path in failingNames) throw IOException("Locked.")
        writes += "delete folder $path"
        val explicit = folders.getOrPut(kind) { mutableSetOf() }
        explicit += withParents(path) - path
        if (stored.values.none { it.kind == kind && (it.folder == path || it.folder.startsWith("$path/")) }) {
            explicit.removeAll { it == path || it.startsWith("$path/") }
        }
    }

    override fun delete(name: String) {
        if (isFailing) throw IOException("Disk full.")
        if (name in failingNames) throw IOException("Locked.")
        writes += "delete $name"
        stored.remove(name)?.let(::keepFolderOf)
    }

    /**
     * Keeps the folder [definition] was in, which stays on disk once the type leaves it.
     */
    private fun keepFolderOf(definition: BlockTypeDefinition) {
        folders.getOrPut(definition.kind) { mutableSetOf() } += withParents(definition.folder)
    }

    private fun withParents(path: String) = path.split('/').filter { it.isNotEmpty() }.runningReduce { parent, name -> "$parent/$name" }
}
