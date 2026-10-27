/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import java.io.File

/**
 * The Gradle init script that adds the `kubrikoCreatorSession` task to a project's build, copied out of the app's
 * resources once per run, since Gradle only accepts init scripts as files.
 */
internal object SessionInitScript {

    const val TASK_NAME = "kubrikoCreatorSession"
    private const val RESOURCE_NAME = "kubriko-creator-session.init.gradle.kts"

    val file: File by lazy {
        File.createTempFile("kubriko-creator-session", ".init.gradle.kts").apply {
            deleteOnExit()
            writeBytes(
                SessionInitScript::class.java.classLoader.getResourceAsStream(RESOURCE_NAME)?.use { it.readBytes() }
                    ?: error("$RESOURCE_NAME is missing from the app's resources."),
            )
        }
    }
}
