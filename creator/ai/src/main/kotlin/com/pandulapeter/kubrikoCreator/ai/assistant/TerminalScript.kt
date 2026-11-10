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
 * command's variables, which may be secret, then runs the command in its working folder and writes its exit code to a file
 * (through a temporary one, so the file is never seen half written). A command that fails leaves the window open, so its
 * output can be read.
 */
internal sealed interface TerminalScript {

    /**
     * The file name extension the system runs a script of this kind by.
     */
    val extension: String

    /**
     * The script's text, running [command] in [workingDirectory] from [script] and writing its exit code to [exitFile].
     */
    fun text(command: AgentCommand, workingDirectory: File, script: File, exitFile: File): String

    data object Posix : TerminalScript {
        override val extension = "command"

        override fun text(command: AgentCommand, workingDirectory: File, script: File, exitFile: File) = buildString {
            val exitFileWhileWritten = File(exitFile.path + PARTIAL)
            appendLine("#!/bin/sh")
            appendLine("rm -f -- ${quoted(script.path)}")
            command.environment.filterKeys { it.matches(variableName) }.forEach { (name, value) -> appendLine("export $name=${quoted(value)}") }
            appendLine("cd ${quoted(workingDirectory.path)}")
            appendLine((listOf(command.program) + command.arguments).joinToString(" ") { quoted(it) })
            appendLine("status=$?")
            appendLine("printf '%s\\n' \"\$status\" > ${quoted(exitFileWhileWritten.path)} && mv -f ${quoted(exitFileWhileWritten.path)} ${quoted(exitFile.path)}")
            appendLine("[ \"\$status\" -eq 0 ] || exec \"\${SHELL:-/bin/sh}\" -i")
        }

        private fun quoted(text: String) = ShellQuoting.posix(text)

        private val variableName = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }

    /**
     * Starts with a byte order mark, without which Windows PowerShell reads a script in the system's code page, which
     * would garble every letter outside it, such as those of a user folder's name in the `PATH`.
     */
    data object PowerShell : TerminalScript {
        override val extension = "ps1"

        override fun text(command: AgentCommand, workingDirectory: File, script: File, exitFile: File) = buildString {
            val exitFileWhileWritten = File(exitFile.path + PARTIAL)
            append(BYTE_ORDER_MARK)
            appendLine("Remove-Item -LiteralPath ${quoted(script.path)} -Force")
            command.environment.forEach { (name, value) -> appendLine("Set-Item -LiteralPath ${quoted("env:$name")} -Value ${quoted(value)}") }
            appendLine("Set-Location -LiteralPath ${quoted(workingDirectory.path)}")
            appendLine("& ${(listOf(command.program) + command.arguments).joinToString(" ") { quoted(it) }}")
            appendLine("\$status = if (\$null -eq \$LASTEXITCODE) { 1 } else { \$LASTEXITCODE }")
            appendLine("Set-Content -LiteralPath ${quoted(exitFileWhileWritten.path)} -Value \$status")
            appendLine("Move-Item -LiteralPath ${quoted(exitFileWhileWritten.path)} -Destination ${quoted(exitFile.path)} -Force")
            appendLine("if (\$status -ne 0) { cmd /c pause }")
        }

        private fun quoted(text: String) = ShellQuoting.powerShell(text)
    }

    private companion object {
        const val PARTIAL = ".partial"
        const val BYTE_ORDER_MARK = '\uFEFF'
    }
}
