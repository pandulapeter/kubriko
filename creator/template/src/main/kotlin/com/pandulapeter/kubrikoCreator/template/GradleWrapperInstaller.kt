/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.gradle.ToolingOutcome
import org.gradle.tooling.GradleConnector
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * Generates the wrapper by running Gradle's own `wrapper` task, so Kubriko Creator doesn't have to ship the wrapper's
 * binary files.
 */
internal object GradleWrapperInstaller : WrapperInstaller {

    override fun install(projectDirectory: File, gradleVersion: String) {
        val cancellation = GradleConnector.newCancellationTokenSource()
        val outcome = ToolingOutcome<Void?>(cancel = cancellation::cancel, stopTimeout = STOP_TIMEOUT)
        val connection = GradleConnector.newConnector()
            .forProjectDirectory(projectDirectory)
            .useGradleVersion(gradleVersion)
            .connect()
        var isStarted = false
        try {
            // The wrapper task writes the version of the Gradle running it, which useGradleVersion pins.
            connection.newBuild()
                .forTasks("wrapper")
                .withCancellationToken(cancellation.token())
                .run(outcome)
            isStarted = true
            outcome.awaitBlocking()
        } finally {
            // Closing waits for the operation, so one that never confirmed its cancel keeps its connection.
            if (outcome.hasEnded || !isStarted) connection.close()
        }
    }

    /**
     * How long a cancelled download is waited for before what was written is removed anyway, so that a Gradle that never
     * confirms the cancel can't hold the creation, the next one, or quitting.
     */
    private val STOP_TIMEOUT = 30.seconds
}
