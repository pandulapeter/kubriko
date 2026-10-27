/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

/**
 * One tool call an AI client made, for the log of the AI connection panel.
 *
 * @param arguments The arguments as the client sent them, in JSON.
 * @param result What the tool returned, or why it was rejected.
 */
internal data class McpToolCall(
    val toolName: String,
    val arguments: String,
    val result: String,
    val isError: Boolean,
)
