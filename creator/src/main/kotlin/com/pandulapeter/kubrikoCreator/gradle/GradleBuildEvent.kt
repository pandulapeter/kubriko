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

import java.io.File

/**
 * Something that happened during a Gradle build run by Kubriko Creator.
 */
internal sealed interface GradleBuildEvent {

    /**
     * One line of the build's console output.
     */
    data class Output(
        val line: String,
        val isError: Boolean,
    ) : GradleBuildEvent

    /**
     * An error reported through Gradle's Problems API, such as a Kotlin compiler error.
     */
    data class Problem(
        val message: String,
        val file: File?,
        val line: Int?,
        val column: Int?,
    ) : GradleBuildEvent
}
