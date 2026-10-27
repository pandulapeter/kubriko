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

/**
 * Groups changed files the way Kubriko Creator thinks of them: an Actor type's definition and its generated code are
 * one change, committed and discarded together.
 */
internal object ChangeGrouping {

    private const val DEFINITIONS_PATH = ".kubriko/actors/"

    /**
     * @param generatedPath Where the project's generated code lives, or null for a project without Actor types.
     */
    fun group(changes: List<FileChange>, generatedPath: String?): List<ChangeEntry> {
        val (actorTypeChanges, fileChanges) = changes.partition { actorTypeName(it.path, generatedPath) != null }
        val actorTypes = actorTypeChanges.groupBy { actorTypeName(it.path, generatedPath)!! }.toSortedMap().map { (name, group) ->
            ChangeEntry(title = name, kind = combinedKind(group.map { it.kind }), paths = group.map { it.path }, isActorType = true)
        }
        val files = fileChanges.map { ChangeEntry(title = it.path, kind = it.kind, paths = listOf(it.path), isActorType = false) }
        return actorTypes + files
    }

    private fun actorTypeName(path: String, generatedPath: String?) = when {
        path.startsWith(DEFINITIONS_PATH) && path.endsWith(".json") -> path.removePrefix(DEFINITIONS_PATH).removeSuffix(".json").takeIf { '/' !in it }
        generatedPath != null && path.startsWith("$generatedPath/") && path.endsWith(".kt") -> path.removePrefix("$generatedPath/").removeSuffix(".kt")
            .takeIf { '/' !in it && it != ActorTypeCodeGenerator.REGISTRY_NAME }
        else -> null
    }

    private fun combinedKind(kinds: List<ChangeKind>) = when {
        ChangeKind.CONFLICTING in kinds -> ChangeKind.CONFLICTING
        kinds.distinct().size == 1 -> kinds.first()
        else -> ChangeKind.MODIFIED
    }
}
