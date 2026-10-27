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
 * Pretends to be the AI tools' programs: the ones in [installed] keep a set of registered servers that `mcp add`,
 * `mcp remove`, `mcp get`, and `mcp list` work on.
 */
internal class FakeProcessRunner(
    val installed: MutableSet<String> = mutableSetOf(),
) : ProcessRunner {
    val calls = mutableListOf<String>()
    val registered = mutableMapOf<String, String>()
    var failingOutput: String? = null

    override fun exists(command: String) = command in installed

    override fun run(command: String, arguments: List<String>): ProcessRunner.Result {
        calls += (listOf(command) + arguments).joinToString(" ")
        if (command !in installed) return ProcessRunner.Result(127, "not found")
        return when (arguments.getOrNull(1)) {
            "add" -> failingOutput?.let { ProcessRunner.Result(1, it) } ?: ProcessRunner.Result(0, "").also { registered[command] = arguments.joinToString(" ") }
            "remove" -> ProcessRunner.Result(if (registered.remove(command) != null) 0 else 1, "")
            "get" -> ProcessRunner.Result(if (command in registered) 0 else 1, "")
            "list" -> ProcessRunner.Result(0, if (command in registered) "kubriko-creator: http://127.0.0.1" else "No MCP servers configured.")
            else -> ProcessRunner.Result(2, "unknown")
        }
    }
}
