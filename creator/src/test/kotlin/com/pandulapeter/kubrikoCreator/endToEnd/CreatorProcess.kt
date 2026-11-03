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
import com.pandulapeter.kubrikoCreator.foundation.SETTINGS_DIRECTORY_PROPERTY
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.testing.TestPorts
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Kubriko Creator running in a JVM of its own, as `:creator:run` starts it, on [projectDirectory], with a settings folder
 * of its own whose MCP server is on, so that [tools] reach it. Everything it prints goes to [log].
 */
internal class CreatorProcess private constructor(
    private val process: Process,
    val tools: CreatorTools,
    val log: File,
) : AutoCloseable {

    val isAlive: Boolean get() = process.isAlive

    /**
     * The last [count] lines Kubriko Creator printed.
     */
    fun logTail(count: Int = 40): String = log.takeIf { it.isFile }?.readLines()?.takeLast(count)?.joinToString("\n").orEmpty()

    /**
     * Waits until the project Kubriko Creator was started with is open.
     */
    fun awaitOpenProject() = tools.awaitOpenProject(isAlive = { isAlive })

    /**
     * Quits Kubriko Creator as the system does at logout, so its shutdown hook ends the game and every process it started,
     * and kills what is left after [QUIT_TIMEOUT_IN_SECONDS].
     */
    override fun close() {
        tools.close()
        val descendants = process.descendants().toList()
        process.destroy()
        if (!process.waitFor(QUIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(QUIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        // Gradle's and Kotlin's daemons outlive their client on purpose, and are shared between the scenarios; anything else Creator started is ended.
        descendants.filter { it.isAlive && !it.isDaemon() }.forEach { it.destroyForcibly() }
    }

    private fun ProcessHandle.isDaemon() = info().arguments().orElse(emptyArray()).any { it.endsWith("GradleDaemon") || it.endsWith("KotlinCompileDaemon") }

    companion object {
        /**
         * The system property the test task sets to the app's runtime classpath (creator/build.gradle.kts).
         */
        const val CLASSPATH_PROPERTY = "kubrikoCreator.runtimeClasspath"
        const val MAIN_CLASS = "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt"
        private const val QUIT_TIMEOUT_IN_SECONDS = 30L

        /**
         * Starts Kubriko Creator on [projectDirectory], keeping its settings and log in [runDirectory], with projects
         * built against [configuration]'s Kubriko.
         */
        fun start(projectDirectory: File, runDirectory: File, configuration: CreatorConfiguration): CreatorProcess {
            val classpath = checkNotNull(System.getProperty(CLASSPATH_PROPERTY)) { "$CLASSPATH_PROPERTY isn't set: run the end-to-end checks through Gradle's :creator:test." }
            val settings = McpSettings(isEnabled = true, port = TestPorts.shared.next(), token = UUID.randomUUID().toString())
            val settingsDirectory = File(runDirectory, "settings")
            writeMcpSettings(settingsDirectory, settings)
            val log = File(runDirectory, "creator.log")
            val process = ProcessBuilder(command(classpath, settingsDirectory, configuration, projectDirectory))
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
            return CreatorProcess(process, CreatorTools(settings), log)
        }

        /**
         * The command starting Kubriko Creator with this JVM's Java.
         */
        internal fun command(classpath: String, settingsDirectory: File, configuration: CreatorConfiguration, projectDirectory: File) = listOf(
            File(System.getProperty("java.home"), "bin/java").path,
            "-D$SETTINGS_DIRECTORY_PROPERTY=${settingsDirectory.absolutePath}",
            "-Dkubriko.creator.kubrikoVersion=${configuration.kubrikoVersion}",
            "-Dkubriko.creator.useMavenLocal=${configuration.useMavenLocal}",
            "-cp",
            classpath,
            MAIN_CLASS,
            projectDirectory.absolutePath,
        )

        /**
         * Turns the MCP server on, on [settings]' port and token, as Kubriko Creator's AI connection panel would.
         */
        internal fun writeMcpSettings(settingsDirectory: File, settings: McpSettings) {
            settingsDirectory.mkdirs()
            File(settingsDirectory, "mcp.json").writeText(Json.encodeToString(McpSettings.serializer(), settings))
        }
    }
}
