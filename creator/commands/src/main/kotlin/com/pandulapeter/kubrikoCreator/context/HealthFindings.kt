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
import com.pandulapeter.kubrikoCreator.commands.scenes.firstSceneSettings
import kotlinx.coroutines.withContext

/**
 * What about the open project is worth a look ([HealthChecks]).
 */
public suspend fun CommandContext.healthFindings(): List<HealthFinding> = healthFindings(sceneTypeCounts()) + withContext(ioDispatcher) { screenplayFindings() }

/**
 * What about the open project is worth a look, with the Scenes' Actor counts [sceneCounts] already read ([sceneTypeCounts]),
 * among every Scene, those that can't be read too.
 */
internal suspend fun CommandContext.healthFindings(sceneCounts: Map<String, Map<String, Int>>): List<HealthFinding> {
    val types = blockTypes?.types?.value.orEmpty()
    val firstScene = withContext(ioDispatcher) { firstSceneSettings(project.rootDirectory)?.firstScene }
    return HealthChecks.of(types, ProjectRelations(types, sceneCounts), scenes.names(), audioFiles(), sceneMusic(), firstScene)
}

/**
 * What in the open project names something that no longer exists ([MissingReferenceChecks]).
 */
public suspend fun CommandContext.missingReferences(): List<HealthFinding> {
    val types = blockTypes?.types?.value.orEmpty()
    val sceneCounts = sceneTypeCounts()
    return withContext(ioDispatcher) { MissingReferenceChecks.of(types, ProjectRelations(types, sceneCounts), scenes.names(), audioFiles(), sceneMusic()) }
}
