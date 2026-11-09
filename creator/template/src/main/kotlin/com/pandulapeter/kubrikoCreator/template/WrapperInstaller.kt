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

import java.io.File

/**
 * Adds the Gradle wrapper (`gradlew` and its files) to a new project. Interrupting the calling thread cancels a download
 * in progress: [install] then throws an [InterruptedException], once Gradle stopped.
 */
public fun interface WrapperInstaller {

    public fun install(projectDirectory: File, gradleVersion: String)
}
