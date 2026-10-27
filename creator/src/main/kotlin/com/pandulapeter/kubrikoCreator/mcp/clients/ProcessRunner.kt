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

/**
 * Runs the command-line programs of AI tools. Every call blocks.
 */
internal interface ProcessRunner {

    /**
     * Whether [command] can be found.
     */
    fun exists(command: String): Boolean

    fun run(command: String, arguments: List<String>): Result

    /**
     * The aliases the user's shell defines, by name, or none where there is no such shell.
     */
    fun aliases(): Map<String, String>

    /**
     * @param output What the program printed, standard output and errors together.
     */
    data class Result(
        val exitCode: Int,
        val output: String,
    )
}
