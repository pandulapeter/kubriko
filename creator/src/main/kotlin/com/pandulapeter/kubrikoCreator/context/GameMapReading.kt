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
 * The digest of the open project and what about it is worth a look, reading its Scenes once for both.
 */
internal suspend fun CommandContext.digestAndHealthFindings(): Pair<ProjectDigest, List<HealthFinding>> {
    val sceneCounts = sceneTypeCounts()
    return projectDigest(sceneCounts) to healthFindings(sceneCounts)
}
