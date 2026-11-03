/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * A [ProjectBuild] whose runs last until the test finishes them.
 */
public class FakeProjectBuild : ProjectBuild {
    /**
     * Every run started, in order.
     */
    public val runs: MutableList<Run> = mutableListOf()

    /**
     * The last run started.
     */
    public val currentRun: Run get() = runs.last()

    /**
     * Whether the build was closed.
     */
    public var isClosed: Boolean = false
        private set

    override suspend fun run(taskPath: String, arguments: List<String>, onEvent: (GradleBuildEvent) -> Unit): Boolean {
        val run = Run(taskPath, arguments, onEvent)
        runs += run
        return try {
            run.result.await()
        } catch (exception: CancellationException) {
            run.isCancelled = true
            throw exception
        }
    }

    override fun close() {
        isClosed = true
    }

    /**
     * One run of a task, which lasts until [finish] is called.
     */
    public class Run(
        public val taskPath: String,
        public val arguments: List<String>,
        public val onEvent: (GradleBuildEvent) -> Unit,
    ) {
        /**
         * What the run returns once finished.
         */
        public val result: CompletableDeferred<Boolean> = CompletableDeferred()

        /**
         * Whether the run was cancelled.
         */
        public var isCancelled: Boolean = false

        /**
         * Ends the run, which returns [isSuccessful].
         */
        public fun finish(isSuccessful: Boolean): Boolean = result.complete(isSuccessful)
    }
}
