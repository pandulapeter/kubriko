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
 * The digest of the open project, as it is now.
 */
internal suspend fun CommandContext.projectDigest() = projectDigest(sceneTypeCounts())

/**
 * The digest of the open project, with the Scenes' Actor counts [sceneCounts] already read ([sceneTypeCounts]).
 */
internal suspend fun CommandContext.projectDigest(sceneCounts: Map<String, Map<String, Int>>) = ProjectDigest.of(
    projectName = project.name,
    types = blockTypes?.types?.value.orEmpty(),
    sceneCounts = sceneCounts,
    sceneDescriptions = withContext(ioDispatcher) { scenes.descriptions.all() },
)
