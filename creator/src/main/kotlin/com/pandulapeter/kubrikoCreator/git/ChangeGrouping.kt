/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.git

import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator

/**
 * Groups changed files the way Kubriko Creator thinks of them: an Actor type's or Manager type's definition and its
 * generated code are one change, committed and discarded together.
 */
internal object ChangeGrouping {

    private val definitionPaths = BlockTypeKind.entries.map { "${BlockTypeStore.CREATOR_DIRECTORY}/${BlockTypeStore.definitionsFolder(it)}/" }
    private val generatedFilesOfNoType = setOf(ActorTypeCodeGenerator.REGISTRY_NAME, ManagerTypeCodeGenerator.WIRING_NAME)

    /**
     * @param generatedPath Where the project's generated code lives, or null for a project without block-built types.
     */
    fun group(changes: List<FileChange>, generatedPath: String?): List<ChangeEntry> {
        val (typeChanges, fileChanges) = changes.partition { typeName(it.path, generatedPath) != null }
        val types = typeChanges.groupBy { typeName(it.path, generatedPath)!! }.toSortedMap().map { (name, group) ->
            ChangeEntry(title = name, kind = combinedKind(group.map { it.kind }), paths = group.map { it.path }, isBlockType = true)
        }
        val files = fileChanges.map { ChangeEntry(title = it.path, kind = it.kind, paths = listOf(it.path), isBlockType = false) }
        return types + files
    }

    private fun typeName(path: String, generatedPath: String?) = when {
        path.endsWith(".json") -> definitionPaths.firstOrNull { path.startsWith(it) }?.let { path.removePrefix(it).removeSuffix(".json").takeIf { name -> '/' !in name } }
        generatedPath != null && path.startsWith("$generatedPath/") && path.endsWith(".kt") -> path.removePrefix("$generatedPath/").removeSuffix(".kt")
            .takeIf { '/' !in it && it !in generatedFilesOfNoType }
        else -> null
    }

    private fun combinedKind(kinds: List<ChangeKind>) = when {
        ChangeKind.CONFLICTING in kinds -> ChangeKind.CONFLICTING
        kinds.distinct().size == 1 -> kinds.first()
        else -> ChangeKind.MODIFIED
    }
}
