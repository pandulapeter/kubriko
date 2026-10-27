/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

/**
 * Runs tasks of a project's build.
 */
internal interface ProjectBuild : AutoCloseable {

    /**
     * Runs [taskPath] and suspends until it finishes. Cancelling the calling coroutine cancels the build, which also
     * stops a running session process.
     *
     * @return Whether the build succeeded.
     */
    suspend fun run(
        taskPath: String,
        arguments: List<String>,
        onEvent: (GradleBuildEvent) -> Unit,
    ): Boolean
}
