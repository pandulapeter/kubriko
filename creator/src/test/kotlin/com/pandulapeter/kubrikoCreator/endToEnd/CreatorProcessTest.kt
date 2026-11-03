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

import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.testing.testConfiguration
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatorProcessTest {

    @Test
    fun creatorStartsOnTheProjectWithItsArgumentsInAFile() {
        val java = File("jbr", "bin/java")
        val arguments = File("runs", "creator.args")
        val project = File("projects", "breakout")
        assertEquals(
            listOf(java.path, "@${arguments.path}", "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt", project.absolutePath),
            CreatorProcess.command(java, arguments, project),
        )
    }

    @Test
    fun itsArgumentsAreTheAppsWithItsOwnSettingsTheGivenKubrikoAndTheClasspath() {
        val configuration = testConfiguration(kubrikoVersion = "9.9.9-creator", useMavenLocal = true)
        val settings = File("runs", "settings")
        assertEquals(
            listOf(
                "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
                "-Dkubriko.creator.settingsDirectory=${settings.absolutePath}",
                "-Dkubriko.creator.kubrikoVersion=9.9.9-creator",
                "-Dkubriko.creator.useMavenLocal=true",
                "-cp",
                "a.jar${File.pathSeparator}b.jar",
            ),
            CreatorProcess.jvmArguments(listOf("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"), "a.jar${File.pathSeparator}b.jar", settings, configuration),
        )
    }

    @Test
    fun theSeededSettingsTurnTheServerOnWithTheirPortAndToken() {
        val directory = File(testDirectory(), "settings")
        CreatorProcess.writeMcpSettings(directory, McpSettings(isEnabled = true, port = 21_000, token = "secret"))
        assertEquals("""{"isEnabled":true,"port":21000,"token":"secret"}""", File(directory, "mcp.json").readText())
    }

    @Test
    fun theTestTaskHandsTheAppsClasspathAndTheJetBrainsRuntime() {
        val classpathFile = System.getProperty(CreatorProcess.CLASSPATH_FILE_PROPERTY)
        assumeTrue("Run through Gradle's :creator:test, which sets ${CreatorProcess.CLASSPATH_FILE_PROPERTY}.", classpathFile != null)
        val classpath = File(classpathFile!!).readText().trim().split(File.pathSeparator)
        assertTrue(classpath.any { File(it).path.endsWith(File("creator", "build/classes/kotlin/main").path) }, classpath.joinToString("\n"))
        assertTrue(classpath.none { File(it).path.contains(File("build", "classes/kotlin/test").path) }, "The tests' own classes are on it.")
        assertTrue(File(System.getProperty(CreatorProcess.JAVA_PROPERTY)).canExecute())
    }
}
