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

/**
 * Every place that refers to [subject] (a type, a Property, a Scene, a page, or a sprite, sound, or music file), in the
 * project's order: what find usages lists, for the user and the AI. The same references a delete of it reads
 * ([ProjectReferences]), so neither ever sees less than the other.
 */
internal suspend fun CommandContext.findUsages(subject: RelationSubject): List<Usage> {
    require(subject !is RelationSubject.Folder) { "A folder's usages are those of what it holds." }
    return projectReferences(subject).usagesOf(subject)
}
