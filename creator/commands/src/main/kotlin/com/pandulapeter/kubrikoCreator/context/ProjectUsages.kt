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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.resources.knownResourcePaths

/**
 * Every place that refers to [subject] (a type, a Property, a Scene, a page, or a sprite, sound, or music file), in the
 * project's order: what find usages lists, for the user and the AI. The same references a delete of it reads
 * ([ProjectReferences]), so neither ever sees less than the other.
 */
internal suspend fun CommandContext.findUsages(subject: RelationSubject): List<Usage> {
    require(subject !is RelationSubject.Folder) { "A folder's usages are those of what it holds." }
    return projectReferences(subject).usagesOf(subject)
}

/**
 * Whether [subject] is still in the project as the last scans know it, so that a view of its usages can tell a renamed
 * or deleted one from one nothing refers to. A file is taken to be there while the files aren't known.
 */
internal fun CommandContext.isInProject(subject: RelationSubject): Boolean = when (subject) {
    is RelationSubject.Type -> blockTypes?.get(subject.name) != null
    is RelationSubject.Property -> blockTypes?.get(subject.owner)?.properties?.any { it.name == subject.name } == true
    is RelationSubject.Scene -> subject.name in scenes.names()
    is RelationSubject.Page -> screenplay.existingName(subject.name) == subject.name
    is RelationSubject.Resource -> knownResourcePaths()?.contains(subject.path) ?: true
    is RelationSubject.Timer, is RelationSubject.Image, is RelationSubject.Folder -> true
}
