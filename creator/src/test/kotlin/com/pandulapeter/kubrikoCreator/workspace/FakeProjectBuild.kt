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

import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException

/**
 * A [ProjectBuild] whose runs last until the test finishes them.
 */
internal class FakeProjectBuild : ProjectBuild {
    val runs = mutableListOf<Run>()
    val currentRun get() = runs.last()
    var isClosed = false
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

    class Run(
        val taskPath: String,
        val arguments: List<String>,
        val onEvent: (GradleBuildEvent) -> Unit,
    ) {
        val result = CompletableDeferred<Boolean>()
        var isCancelled = false

        fun finish(isSuccessful: Boolean) = result.complete(isSuccessful)
    }
}
