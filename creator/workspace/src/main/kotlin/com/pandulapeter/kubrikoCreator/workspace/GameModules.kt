/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

/**
 * The modules a project's game could be (those with a Compose desktop `run` task, in Gradle's order) and the one it is.
 */
public data class GameModules(
    val available: List<String>,
    val chosen: String,
) {
    /**
     * Whether there is more than one module to choose from, which is only then offered.
     */
    public val isChoosable: Boolean get() = available.size > 1
}
