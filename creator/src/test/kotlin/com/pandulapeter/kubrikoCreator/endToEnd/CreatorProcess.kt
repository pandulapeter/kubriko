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
import com.pandulapeter.kubrikoCreator.session.JavaArgumentFile
import com.pandulapeter.kubrikoCreator.testing.TestPorts
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Kubriko Creator running in a JVM of its own, as `:creator:run` starts it (with its JetBrains Runtime, JVM arguments,
 * and runtime classpath), with a settings folder of its own whose MCP server is on, so that [tools] reach it. Everything
 * it prints goes to [log].
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
         * The system properties the test task sets (creator/build.gradle.kts): the file holding the app's runtime classpath,
         * the JetBrains Runtime's `java`, and the JVM arguments, one per line, that `:creator:run` starts the app with.
         */
        const val CLASSPATH_FILE_PROPERTY = "kubrikoCreator.runtimeClasspathFile"
        const val JAVA_PROPERTY = "kubrikoCreator.java"
        const val JVM_ARGUMENTS_PROPERTY = "kubrikoCreator.jvmArguments"
        const val MAIN_CLASS = "com.pandulapeter.kubrikoCreator.KubrikoCreatorAppKt"
        private const val QUIT_TIMEOUT_IN_SECONDS = 30L

        /**
         * Starts Kubriko Creator on [projectDirectory], keeping its settings, arguments, and log in [runDirectory], with
         * projects built against [configuration]'s Kubriko.
         */
        fun start(projectDirectory: File, runDirectory: File, configuration: CreatorConfiguration): CreatorProcess {
            val classpathFile = File(requiredProperty(CLASSPATH_FILE_PROPERTY))
            val jvmArguments = System.getProperty(JVM_ARGUMENTS_PROPERTY).orEmpty().lines().filter { it.isNotBlank() }
            val settings = McpSettings(isEnabled = true, port = TestPorts.shared.next(), token = UUID.randomUUID().toString())
            val settingsDirectory = File(runDirectory, "settings")
            writeMcpSettings(settingsDirectory, settings)
            // The classpath goes through an @argfile, since it is longer than Windows lets a command line be.
            val argumentFile = File(runDirectory, "creator.args").apply {
                writeText(JavaArgumentFile.format(jvmArguments(jvmArguments, classpathFile.readText().trim(), settingsDirectory, configuration)))
            }
            val log = File(runDirectory, "creator.log")
            val process = ProcessBuilder(command(File(requiredProperty(JAVA_PROPERTY)), argumentFile, projectDirectory))
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
            return CreatorProcess(process, CreatorTools(settings), log)
        }

        private fun requiredProperty(name: String) = checkNotNull(System.getProperty(name)) { "$name isn't set: run the end-to-end checks through Gradle's :creator:test." }

        /**
         * The command starting Kubriko Creator with [java], with the JVM's arguments and classpath in [argumentFile].
         */
        internal fun command(java: File, argumentFile: File, projectDirectory: File) = listOf(java.path, "@${argumentFile.path}", MAIN_CLASS, projectDirectory.absolutePath)

        /**
         * What [command]'s `@argfile` holds: [appArguments] (those of `:creator:run`), the system properties giving
         * Kubriko Creator [settingsDirectory] and [configuration]'s Kubriko, and [classpath].
         */
        internal fun jvmArguments(appArguments: List<String>, classpath: String, settingsDirectory: File, configuration: CreatorConfiguration) = appArguments + listOf(
            "-D$SETTINGS_DIRECTORY_PROPERTY=${settingsDirectory.absolutePath}",
            "-Dkubriko.creator.kubrikoVersion=${configuration.kubrikoVersion}",
            "-Dkubriko.creator.useMavenLocal=${configuration.useMavenLocal}",
            "-cp",
            classpath,
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
