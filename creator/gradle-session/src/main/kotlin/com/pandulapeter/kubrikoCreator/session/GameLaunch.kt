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
 * @param reloadTaskName The task that applies changes to the running game, for a game started with Compose Hot Reload
 *   ([HotReloadLaunch]), which keeps a copy of the module's classes of its own; null for one started from the module's
 *   classes.
 */
public data class GameLaunch(
    public val java: File,
    public val mainClass: String,
    public val workingDirectory: File,
    public val jvmArguments: List<String>,
    public val classpath: List<File>,
    public val reloadTaskName: String? = null,
) {
    /**
     * The command that starts the session's launcher with the classpath written to [classpathArgumentFile]
     * ([classpathArguments]) and [systemProperties], playing [mainClass]. The classpath goes through a Java `@argfile`,
     * since a long one would pass the length Windows allows a command line.
     */
    internal fun command(classpathArgumentFile: File, systemProperties: Map<String, String>) = listOf(java.path) +
        jvmArguments +
        systemProperties.map { (key, value) -> "-D$key=$value" } +
        listOf("@${classpathArgumentFile.path}", LAUNCHER_CLASS, "play", mainClass)

    internal companion object {

        /**
         * The text of the `@argfile` that sets [classpath] for [command].
         */
        fun classpathArguments(classpath: List<File>) = JavaArgumentFile.format(listOf("-cp", classpath.joinToString(File.pathSeparator) { it.path }))

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
