/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.json.JsonObject

/**
 * A command as an AI tool: what the MCP server lists and the assistant panel sends to the model.
 *
 * @param isReadOnly Whether calling the tool leaves the project, and everything else, as it is.
 * @param readsProject Whether the tool reads the project, so that a conversation calling it again hears what changed.
 * @param isDestructive Whether calling the tool throws work away for good.
 * @param isAlwaysLoaded Whether a client that loads tool schemas only when the model asks for them (Claude Code's tool
 *   search) should load this one from the start, as one nearly every conversation uses.
 */
public data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val isReadOnly: Boolean,
    val isDestructive: Boolean,
    val isAlwaysLoaded: Boolean = false,
    val readsProject: Boolean = false,
)
