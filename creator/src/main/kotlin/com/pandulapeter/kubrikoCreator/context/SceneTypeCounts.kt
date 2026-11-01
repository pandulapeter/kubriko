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
import kotlinx.coroutines.withContext

/**
 * The number of Actors of each type ID that each readable Scene of the project places, by the Scene's name.
 */
internal suspend fun CommandContext.sceneTypeCounts(): Map<String, Map<String, Int>> = withContext(ioDispatcher) {
    scenes.names().mapNotNull { name -> scenes.actors(name)?.let { actors -> name to actors.groupingBy { it.typeId }.eachCount().toSortedMap() } }.toMap()
}

/**
 * The relations between the parts of the open project.
 */
internal suspend fun CommandContext.projectRelations() = ProjectRelations(types = blockTypes?.types?.value.orEmpty(), scenes = sceneTypeCounts())
