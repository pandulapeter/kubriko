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

import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import org.junit.Assume.assumeTrue
import java.awt.GraphicsEnvironment
import java.io.File

/**
 * Runs the end-to-end checks' scenarios, which only run when the [SWITCH] environment variable is `true`, on a display:
 * on Linux under `xvfb-run -s "-screen 0 1920x1080x24"`. Projects are built against the Kubriko that
 * `KUBRIKO_CREATOR_VERSION` names (published to Maven Local), or the bundled one.
 */
internal object EndToEnd {
    const val SWITCH = "KUBRIKO_CREATOR_E2E"
    private const val VERSION_VARIABLE = "KUBRIKO_CREATOR_VERSION"

    /**
     * Runs the scenario [name] on a fresh Breakout copy, opened in a Kubriko Creator of its own, which quits afterwards.
     * Its images go to `creator/build/end-to-end/<name>`.
     */
    fun scenario(name: String, block: EndToEndScenario.() -> Unit) {
        assumeTrue("Set $SWITCH=true to run the end-to-end checks.", System.getenv(SWITCH) == "true")
        check(!GraphicsEnvironment.isHeadless()) { "The end-to-end checks need a display: on Linux, run them under xvfb-run -s \"-screen 0 1920x1080x24\"." }
        val outputDirectory = File(System.getProperty("kubrikoCreator.directory"), "build/end-to-end/$name").apply {
            deleteRecursively()
            mkdirs()
        }
        val configuration = configuration()
        val project = BreakoutProject.create(testDirectory(), configuration)
        CreatorProcess.start(project, outputDirectory, configuration).use { creator ->
            val scenario = EndToEndScenario(name, project, outputDirectory, creator.tools, creator::logTail)
            scenario.step("Kubriko Creator opens the project") { creator.awaitOpenProject() }
            scenario.block()
        }
    }

    private fun configuration() = CreatorConfiguration.load().let { bundled ->
        System.getenv(VERSION_VARIABLE)?.let { bundled.copy(kubrikoVersion = it, useMavenLocal = true) } ?: bundled
    }
}
