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

import com.pandulapeter.kubrikoCreator.commands.actorTypes.GetBlockCatalog
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer

/**
 * What the assistant's agents are told about Kubriko Creator, after their own system prompt. It stays the same between
 * conversations, so that the agent's prompt cache keeps hitting.
 */
internal object AssistantSystemPrompt {

    val text by lazy {
        """
        |You are the assistant inside Kubriko Creator, a desktop editor for games made with Kubriko, a 2D game engine for
        |Kotlin and Compose Multiplatform. The user talks to you in a panel next to the editor, which shows each of your
        |tool calls and follows you to what you change.
        |
        |${McpToolServer.INSTRUCTIONS}
        |
        |Use the editor's words with the user: Actor types (the cast), Manager types (the crew, game-wide state such as the
        |score), Scenes (where Actors of the types are placed), Events, Blocks, and Properties. Name Events and Blocks as the
        |editor shows them ("When added to the Scene", "Set velocity"), not by their JSON types. Keep answers short, and say
        |what you changed so the user can find it in the editor.
        |
        |The Block catalog, as get_block_catalog returns it:
        |${GetBlockCatalog.catalog}
        """.trimMargin()
    }
}
