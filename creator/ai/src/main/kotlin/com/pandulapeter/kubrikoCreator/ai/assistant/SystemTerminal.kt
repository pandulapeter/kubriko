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

import com.pandulapeter.kubrikoCreator.foundation.findProgram
import com.pandulapeter.kubrikoCreator.foundation.pathFolders
import java.io.File

/**
 * The operating system's own terminal, which opens a [TerminalScript] in a window of its own, for a sign-in command that
 * needs a terminal device, which the assistant panel's pipes aren't.
 */
internal sealed interface SystemTerminal {

    /**
     * The kind of script it runs.
     */
    val script: TerminalScript

    /**
     * What opens [script] in a new window.
     */
    fun openCommand(script: File): List<String>

    /**
     * Terminal, named rather than left to what opens `.command` files, which may be an editor that would show the script's
     * variables and never run it.
     */
    data object Mac : SystemTerminal {
        override val script = TerminalScript.Posix

        override fun openCommand(script: File) = listOf("open", "-a", "Terminal", script.path)
    }

    /**
     * @param prefix The terminal emulator and the option it runs a command with.
     */
    data class Linux(
        val prefix: List<String>,
    ) : SystemTerminal {
        override val script = TerminalScript.Posix

        override fun openCommand(script: File) = prefix + listOf("sh", script.path)
    }

    /**
     * A PowerShell window, started through `cmd`'s `start`, which gives it a console of its own.
     */
    data object Windows : SystemTerminal {
        override val script = TerminalScript.PowerShell

        override fun openCommand(script: File) = listOf(
            "cmd", "/c", "start", "\"\"", "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.path,
        )
    }

    companion object {
        /**
         * The terminal emulators tried on Linux, in order, with the option each runs a command with: the one Debian's
         * alternatives choose first, then the common desktops' own, then xterm.
         */
        private val linuxTerminals = listOf(
            listOf("x-terminal-emulator", "-e"),
            listOf("gnome-terminal", "--"),
            listOf("konsole", "-e"),
            listOf("xfce4-terminal", "-x"),
            listOf("xterm", "-e"),
        )

        /**
         * This computer's terminal, or null on Linux when no known terminal emulator is on [path].
         *
         * @param operatingSystem The `os.name` system property, lowercase.
         * @param path The folders programs are looked for in.
         */
        fun find(
            operatingSystem: String = System.getProperty("os.name").orEmpty().lowercase(),
            path: List<File> = pathFolders(),
        ): SystemTerminal? = when {
            "mac" in operatingSystem -> Mac
            "windows" in operatingSystem -> Windows
            else -> linuxTerminals.firstOrNull { (program) -> findProgram(program, path) != null }?.let(::Linux)
        }
    }
}
