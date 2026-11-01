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

import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer

/**
 * Recognizes Kubriko Creator's own tools in the titles agents give tool calls, which each agent writes its own way:
 * Claude Code as `mcp__kubriko-creator__read_scene`, Google Antigravity as `kubriko-creator_read_scene`.
 */
internal object CreatorToolName {
    private val pattern = Regex("^(?:mcp__)?" + Regex.escape(McpToolServer.SERVER_NAME) + "(?:__|_|/|\\.|:)([a-z][a-z0-9_]*)$")

    /**
     * The name of the Kubriko Creator tool [title] names, or null if it names none.
     */
    fun of(title: String?): String? = title?.trim()?.let(pattern::matchEntire)?.groupValues?.get(1)?.takeIf { CommandRegistry.editor[it] != null }
}
