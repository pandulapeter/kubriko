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

/**
 * What programs print when they need a terminal device and get pipes: the C library's own error, Node's Ink, which
 * interactive command-line tools draw with, and the wording of tools that check for one first.
 */
internal object TerminalDeviceErrors {
    private val patterns = listOf(
        "not a tty",
        "inappropriate ioctl for device",
        "raw mode is not supported",
        "requires a tty",
        "requires an interactive terminal",
        "requires a terminal",
        "must be run in a terminal",
        "stdin is not a terminal",
        "input is not a terminal",
        "no tty present",
    )

    /**
     * Whether [output] says that the program that printed it needed a terminal device.
     */
    fun areIn(output: String): Boolean = output.lowercase().let { text -> patterns.any { it in text } }
}
