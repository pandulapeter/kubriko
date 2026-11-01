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
 * One tool call an AI client made, for the log of the AI connection panel. Only the start of the arguments and the result
 * is kept, since a result can be a whole project and the log holds hundreds of calls.
 *
 * @param arguments The start of the arguments as the client sent them, in JSON.
 * @param result The start of what the tool returned, or why it was rejected.
 */
internal data class McpToolCall(
    val toolName: String,
    val arguments: String,
    val result: String,
    val isError: Boolean,
) {
    companion object {
        const val PREVIEW_LENGTH = 300

        fun of(toolName: String, arguments: String, result: String, isError: Boolean) = McpToolCall(toolName, arguments.preview(), result.preview(), isError)

        private fun String.preview() = if (length <= PREVIEW_LENGTH) this else take(PREVIEW_LENGTH) + "…"
    }
}
