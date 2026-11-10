/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.mcp.clients.ShellQuoting
import java.io.File

/**
 * A script that runs a command in the system's terminal ([SystemTerminal]): it deletes itself first, since it holds the
 * command's variables, which may be secret, writes its own process ID to a file, so Creator can tell when its window was
 * closed, then runs the command in its working folder and writes its exit code to another file. Both files are written
 * through a temporary one, so they are never seen half written. A command that fails leaves the window open, so its output
 * can be read.
 */
internal sealed interface TerminalScript {

    /**
     * The file name extension the system runs a script of this kind by.
     */
    val extension: String

    /**
     * The script's text, running [command] in [workingDirectory] from [files]' script.
     */
    fun text(command: AgentCommand, workingDirectory: File, files: TerminalScriptFiles): String

    data object Posix : TerminalScript {
        override val extension = "command"

        override fun text(command: AgentCommand, workingDirectory: File, files: TerminalScriptFiles) = buildString {
            appendLine("#!/bin/sh")
            appendLine("rm -f -- ${quoted(files.script.path)}")
            appendLine(written("\"\$\$\"", files.processId))
            command.environment.filterKeys { it.matches(variableName) }.forEach { (name, value) -> appendLine("export $name=${quoted(value)}") }
            appendLine("if cd ${quoted(workingDirectory.path)}; then")
            appendLine("    " + (listOf(command.program) + command.arguments).joinToString(" ") { quoted(it) })
            appendLine("    status=$?")
            appendLine("else")
            appendLine("    status=1")
            appendLine("fi")
            appendLine(written("\"\$status\"", files.exitCode))
            appendLine("[ \"\$status\" -eq 0 ] || exec \"\${SHELL:-/bin/sh}\" -i")
        }

        private fun written(value: String, file: File): String {
            val partial = quoted(file.path + PARTIAL)
            return "printf '%s\\n' $value > $partial && mv -f $partial ${quoted(file.path)}"
        }

        private fun quoted(text: String) = ShellQuoting.posix(text)

        private val variableName = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }

    /**
     * Starts with a byte order mark, without which Windows PowerShell reads a script in the system's code page, which
     * would garble every letter outside it, such as those of a user folder's name in the `PATH`. Arguments reach the
     * program the legacy way on every PowerShell version, which drops double quotes inside them, so those are escaped as
     * Windows programs read them back ([nativeArgument]).
     */
    data object PowerShell : TerminalScript {
        override val extension = "ps1"

        override fun text(command: AgentCommand, workingDirectory: File, files: TerminalScriptFiles) = buildString {
            append(BYTE_ORDER_MARK)
            appendLine("Remove-Item -LiteralPath ${quoted(files.script.path)} -Force")
            appendLine(written("\$PID", files.processId))
            appendLine("\$PSNativeCommandArgumentPassing = 'Legacy'")
            command.environment.forEach { (name, value) -> appendLine("Set-Item -LiteralPath ${quoted("env:$name")} -Value ${quoted(value)}") }
            appendLine("try {")
            appendLine("    Set-Location -LiteralPath ${quoted(workingDirectory.path)} -ErrorAction Stop")
            appendLine("    & ${(listOf(quoted(command.program)) + command.arguments.map { quoted(nativeArgument(it)) }).joinToString(" ")}")
            appendLine("    \$status = if (\$null -eq \$LASTEXITCODE) { 1 } else { \$LASTEXITCODE }")
            appendLine("} catch {")
            appendLine("    Write-Host \$_")
            appendLine("    \$status = 1")
            appendLine("}")
            appendLine(written("\$status", files.exitCode))
            appendLine("if (\$status -ne 0) { cmd /c pause }")
        }

        private fun written(value: String, file: File): String {
            val partial = quoted(file.path + PARTIAL)
            return "Set-Content -LiteralPath $partial -Value $value; Move-Item -LiteralPath $partial -Destination ${quoted(file.path)} -Force"
        }

        private fun quoted(text: String) = ShellQuoting.powerShell(text)

        /**
         * [argument] escaped so that a Windows program reads it back as it is from the command line PowerShell builds,
         * which wraps an argument holding whitespace in double quotes: each double quote is escaped with a backslash, the
         * backslashes before it doubled, and so are those at the end of an argument that gets wrapped.
         */
        internal fun nativeArgument(argument: String): String {
            val escaped = argument.replace(quoteWithBackslashes) { match -> match.groupValues[1].repeat(2) + "\\\"" }
            return if (argument.any { it.isWhitespace() }) escaped.replace(trailingBackslashes) { it.value.repeat(2) } else escaped
        }

        private val quoteWithBackslashes = Regex("(\\\\*)\"")
        private val trailingBackslashes = Regex("\\\\+$")
    }

    private companion object {
        const val PARTIAL = ".partial"
        const val BYTE_ORDER_MARK = '\uFEFF'
    }
}

/**
 * The files of one [TerminalScript]: the script itself, and where it writes its process ID and the command's exit code.
 */
internal data class TerminalScriptFiles(
    val script: File,
    val processId: File,
    val exitCode: File,
)
