/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import java.io.File

/**
 * A scenario's failure, naming its step, with what helps tell why: the last images it saved and Kubriko Creator's log.
 */
internal class ScenarioFailure(message: String, cause: Throwable) : AssertionError(message, cause) {

    companion object {
        fun of(scenario: String, step: String, problem: Throwable, lastImages: List<File>, log: String) = ScenarioFailure(
            message = buildString {
                appendLine("\"$scenario\" failed at \"$step\": ${problem.message ?: problem::class.simpleName}")
                appendLine(if (lastImages.isEmpty()) "No images were taken before it." else "The last images: ${lastImages.joinToString { it.path }}")
                append("The end of Kubriko Creator's log:\n$log")
            },
            cause = problem,
        )
    }
}
