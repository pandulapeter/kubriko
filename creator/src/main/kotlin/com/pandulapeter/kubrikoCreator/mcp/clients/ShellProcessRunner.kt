/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Runs programs the way a terminal would find them. An app started from the Finder or the Start menu doesn't have the
 * PATH of the user's shell, where tools installed with npm or Homebrew live, so on macOS and Linux every command goes
 * through the user's login shell.
 */
internal class ShellProcessRunner(
    private val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("windows"),
    private val shell: String = System.getenv("SHELL")?.takeIf { it.isNotBlank() } ?: "/bin/sh",
    private val timeoutInSeconds: Long = 30,
) : ProcessRunner {

    override fun exists(command: String) = executableOf(command).let { executable ->
        run(if (isWindows) listOf("where", executable) else shellCommand("command -v ${quoted(executable)}")).exitCode == 0
    }

    /**
     * Runs [command] as the user typed it (so it can set environment variables first, like `CLAUDE_CONFIG_DIR=… claude`),
     * followed by [arguments] passed as they are.
     */
    override fun run(command: String, arguments: List<String>) = run(
        if (isWindows) listOf("cmd", "/c") + command.trim().split(whitespace) + arguments else shellCommand(command.trim() + arguments.joinToString("") { " " + quoted(it) }),
    )

    private fun shellCommand(script: String) = listOf(shell, "-l", "-c", script)

    private fun run(commandLine: List<String>) = try {
        val process = ProcessBuilder(commandLine).redirectErrorStream(true).redirectInput(ProcessBuilder.Redirect.from(File(if (isWindows) "NUL" else "/dev/null"))).start()
        val output = StringBuilder()
        val reader = Thread { output.append(process.inputStream.bufferedReader().readText()) }.apply { start() }
        if (process.waitFor(timeoutInSeconds, TimeUnit.SECONDS)) {
            reader.join()
            ProcessRunner.Result(process.exitValue(), output.toString().trim())
        } else {
            process.destroyForcibly()
            ProcessRunner.Result(TIMED_OUT, "")
        }
    } catch (exception: IOException) {
        ProcessRunner.Result(NOT_STARTED, exception.message.orEmpty())
    }

    private companion object {
        const val TIMED_OUT = -1
        const val NOT_STARTED = -2
        val whitespace = Regex("\\s+")

        /**
         * The program [command] runs: its first word that doesn't set an environment variable.
         */
        fun executableOf(command: String) = command.trim().split(whitespace).firstOrNull { '=' !in it }.orEmpty()

        /**
         * Quotes [argument] for a POSIX shell, so that tokens and URLs are passed as they are.
         */
        fun quoted(argument: String) = "'" + argument.replace("'", "'\\''") + "'"
    }
}
