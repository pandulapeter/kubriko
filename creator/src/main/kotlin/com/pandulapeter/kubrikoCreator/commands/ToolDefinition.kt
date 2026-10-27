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
 * @param isReadOnly Whether calling the tool leaves the project as it is.
 * @param isDestructive Whether calling the tool throws work away for good.
 */
internal data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val isReadOnly: Boolean,
    val isDestructive: Boolean,
)
