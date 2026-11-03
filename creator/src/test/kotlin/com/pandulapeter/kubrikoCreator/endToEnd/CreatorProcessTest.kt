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
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatorProcessTest {

    @Test
    fun creatorStartsOnTheProjectWithItsOwnSettingsAndTheGivenKubriko() {
        val configuration = CreatorConfiguration(kubrikoVersion = "9.9.9-creator", kotlinVersion = "2.4.21", composeVersion = "1.12.1", gradleVersion = "9.7.1", useMavenLocal = true)
        val settings = File("runs", "settings")
        val project = File("projects", "breakout")
        assertEquals(
            listOf(
                File(System.getProperty("java.home"), "bin/java").path,
                "-Dkubriko.creator.settingsDirectory=${settings.absolutePath}",
                "-Dkubriko.creator.kubrikoVersion=9.9.9-creator",
                "-Dkubriko.creator.useMavenLocal=true",
                "-cp",
                "a.jar${File.pathSeparator}b.jar",
                "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt",
                project.absolutePath,
            ),
            CreatorProcess.command("a.jar${File.pathSeparator}b.jar", settings, configuration, project),
        )
    }

    @Test
    fun theSeededSettingsTurnTheServerOnWithTheirPortAndToken() {
        val directory = File(testDirectory(), "settings")
        CreatorProcess.writeMcpSettings(directory, McpSettings(isEnabled = true, port = 21_000, token = "secret"))
        assertEquals("""{"isEnabled":true,"port":21000,"token":"secret"}""", File(directory, "mcp.json").readText())
    }

    @Test
    fun theTestTaskHandsTheAppsClasspath() {
        val classpath = checkNotNull(System.getProperty(CreatorProcess.CLASSPATH_PROPERTY)).split(File.pathSeparator)
        assertTrue(classpath.any { File(it).path.endsWith(File("creator", "build/classes/kotlin/main").path) }, classpath.joinToString("\n"))
        assertTrue(classpath.none { File(it).path.contains(File("build", "classes/kotlin/test").path) }, "The tests' own classes are on it.")
    }
}
