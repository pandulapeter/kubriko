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
 * What about the open project is worth a look ([HealthChecks]).
 */
internal suspend fun CommandContext.healthFindings(): List<HealthFinding> = healthFindings(sceneTypeCounts())

/**
 * What about the open project is worth a look, with the Scenes' Actor counts [sceneCounts] already read ([sceneTypeCounts]).
 */
internal fun CommandContext.healthFindings(sceneCounts: Map<String, Map<String, Int>>): List<HealthFinding> {
    val types = blockTypes?.types?.value.orEmpty()
    return HealthChecks.of(types, ProjectRelations(types, sceneCounts), sceneCounts.keys)
}
