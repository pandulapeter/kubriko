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
 * The paths from the project's root of the pages that mention the types and Scenes among [targets], so that the agent
 * that changed them knows which pages may need the same change. Reads the disk.
 */
internal fun CommandContext.pagesMentioning(targets: Collection<CommandTarget>): List<String> {
    val paths = targets.mapNotNull { target ->
        when (target) {
            is CommandTarget.BlockType -> blockTypes?.get(target.name)?.let(DefinitionPaths::definitionPath)
            is CommandTarget.Scene -> scenes.fileOf(target.name).takeIf { it.isFile }?.let(::projectPathOf)
            else -> null
        }
    }
    if (paths.isEmpty()) return emptyList()
    val index = mentionIndex()
    return paths.flatMap(index::pagesMentioning).distinct().map(PageNaming::pathOf)
}
