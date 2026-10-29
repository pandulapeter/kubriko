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
 * How the init script's `kubrikoCreatorSession` task would start a play session, as its classpath task writes it (the
 * `launch.` lines of the classpath file): Kubriko Creator starts the game the same way itself, without Gradle.
 *
 * @param java The Java executable of the desktop module's `run` task.
 * @param jvmArguments The `run` task's JVM arguments.
 * @param classpath The module's own classes and resources, then the session library's runtime classpath.
 */
internal data class GameLaunch(
    val java: File,
    val mainClass: String,
    val workingDirectory: File,
    val jvmArguments: List<String>,
    val classpath: List<File>,
) {
    /**
     * The command that starts the session's launcher with [classpath] and [systemProperties], playing [mainClass].
     */
    fun command(classpath: List<File>, systemProperties: Map<String, String>) = listOf(java.path) +
        jvmArguments +
        systemProperties.map { (key, value) -> "-D$key=$value" } +
        listOf("-cp", classpath.joinToString(File.pathSeparator) { it.path }, LAUNCHER_CLASS, "play", mainClass)

    companion object {
        private const val LAUNCHER_CLASS = "com.pandulapeter.kubriko.creatorSession.CreatorSessionLauncherKt"
        const val PREFIX = "launch."

        /**
         * The launch described by the `launch.` [lines] of a classpath file, or null if they don't describe one (a module
         * whose `run` task has no Java executable or main class).
         */
        fun parse(lines: List<String>): GameLaunch? {
            fun values(key: String) = lines.filter { it.startsWith("$PREFIX$key=") }.map { it.substringAfter('=') }
            return GameLaunch(
                java = File(values("java").singleOrNull() ?: return null),
                mainClass = values("mainClass").singleOrNull() ?: return null,
                workingDirectory = File(values("workingDir").singleOrNull() ?: return null),
                jvmArguments = values("jvmArg"),
                classpath = values("classpath").map(::File).ifEmpty { return null },
            )
        }
    }
}
