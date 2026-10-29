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
 * How Compose Hot Reload would run the game, as the classpath task writes it for a play session (the `launch.hot` lines),
 * so that Kubriko Creator can start the game with its agent and apply changes to it through [reloadTaskName].
 *
 * @param java The JetBrains Runtime of Hot Reload's run task, whose class redefinition Hot Reload needs.
 * @param argumentFile The `@argfile` Hot Reload's run task runs: its agent and settings, its own copy of the module's
 *   classes, the runtime classpath, and the main class.
 * @param reloadTaskName The task that compiles the module and hands the changed classes to the running game.
 * @param libraries The session library's runtime classpath, resolved on its own (which may raise library versions).
 */
internal data class HotReloadLaunch(
    val java: File,
    val argumentFile: File,
    val reloadTaskName: String,
    val libraries: List<File>,
) {
    /**
     * [launch] started as Hot Reload starts the game, given the [argumentFile]'s text and the module's runtime classpath
     * ([runtimeFiles], which [libraries] replace), or null if the argument file has no classpath.
     */
    fun applyTo(launch: GameLaunch, argumentFileText: String, runtimeFiles: Collection<File>): GameLaunch? {
        val arguments = JavaArgumentFile.parse(argumentFileText)
        val classpathIndex = arguments.indexOfFirst { it in CLASSPATH_OPTIONS }.takeIf { it >= 0 && it + 1 < arguments.size } ?: return null
        val runtime = runtimeFiles.toSet()
        val hotFiles = arguments[classpathIndex + 1].split(File.pathSeparator).filter { it.isNotEmpty() }.map(::File).filterNot { it in runtime }
        return launch.copy(
            java = java,
            // What follows the classpath ends with the main class, which the session's launcher is given instead.
            jvmArguments = arguments.take(classpathIndex) + arguments.drop(classpathIndex + 2).dropLast(1) + DEV_TOOLS_OFF + launch.jvmArguments,
            classpath = (hotFiles + libraries).distinct(),
            reloadTaskName = reloadTaskName,
        )
    }

    companion object {
        private val CLASSPATH_OPTIONS = setOf("-cp", "-classpath", "--class-path")

        /**
         * Hot Reload's DevTools window would open next to the game.
         */
        private const val DEV_TOOLS_OFF = "-Dcompose.reload.devToolsEnabled=false"
        const val PREFIX = "${GameLaunch.PREFIX}hot"

        /**
         * The Hot Reload launch the `launch.hot` [lines] of a classpath file describe, or null if they don't describe one.
         */
        fun parse(lines: List<String>): HotReloadLaunch? {
            fun values(key: String) = lines.filter { it.startsWith("$PREFIX$key=") }.map { it.substringAfter('=') }
            return HotReloadLaunch(
                java = File(values("Java").singleOrNull() ?: return null),
                argumentFile = File(values("ArgumentFile").singleOrNull() ?: return null),
                reloadTaskName = values("ReloadTask").singleOrNull() ?: return null,
                libraries = values("Library").map(::File),
            )
        }
    }
}
