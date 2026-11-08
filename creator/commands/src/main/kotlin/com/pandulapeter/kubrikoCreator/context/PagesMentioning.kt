/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.screenplay.projectPathOf
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming

/**
 * The paths from the project's root of the files of the types and Scenes among [targets] that exist now: what a change
 * to them may leave a page saying about them. Reads the disk.
 */
internal fun CommandContext.mentionPathsOf(targets: Collection<CommandTarget>): Set<String> = targets.mapNotNullTo(LinkedHashSet()) { target ->
    when (target) {
        is CommandTarget.BlockType -> blockTypes?.get(target.name)?.let(DefinitionPaths::definitionPath)
        is CommandTarget.Scene -> scenes.fileOf(target.name).takeIf { it.isFile }?.let(::projectPathOf)
        else -> null
    }
}

/**
 * The file a page links to to mention [subject], a path from the project's root, if it exists and can be mentioned. Reads
 * the disk.
 */
internal fun CommandContext.mentionedPathOf(subject: RelationSubject): String? = when (subject) {
    is RelationSubject.Type -> mentionPathsOf(listOf(CommandTarget.BlockType(subject.name))).firstOrNull()
    is RelationSubject.Scene -> mentionPathsOf(listOf(CommandTarget.Scene(subject.name))).firstOrNull()
    is RelationSubject.Page -> PageNaming.pathOf(subject.name)
    is RelationSubject.Property, is RelationSubject.Image -> null
}

/**
 * The paths from the project's root of the pages that mention any of [paths], so that the agent that changed what is
 * there knows which pages may need the same change. Reads the disk.
 */
internal fun CommandContext.pagesMentioning(paths: Collection<String>): List<String> {
    if (paths.isEmpty()) return emptyList()
    val index = mentionIndex()
    return paths.flatMap(index::pagesMentioning).distinct().map(PageNaming::pathOf)
}
