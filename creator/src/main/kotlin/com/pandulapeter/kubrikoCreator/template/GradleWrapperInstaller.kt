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

import org.gradle.tooling.GradleConnector
import java.io.File

/**
 * Generates the wrapper by running Gradle's own `wrapper` task, so Kubriko Creator doesn't have to ship the wrapper's
 * binary files.
 */
internal object GradleWrapperInstaller : WrapperInstaller {

    override fun install(projectDirectory: File, gradleVersion: String) = GradleConnector.newConnector()
        .forProjectDirectory(projectDirectory)
        .useGradleVersion(gradleVersion)
        .connect()
        .use { connection ->
            // The wrapper task writes the version of the Gradle running it, which useGradleVersion pins.
            connection.newBuild()
                .forTasks("wrapper")
                .run()
        }
}
