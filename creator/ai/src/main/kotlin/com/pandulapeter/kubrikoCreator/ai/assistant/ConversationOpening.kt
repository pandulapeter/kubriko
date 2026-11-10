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

import kotlinx.serialization.json.JsonElement

/**
 * What a new conversation is started with besides its tools: the system prompt reaches the agent through one of them.
 *
 * @param meta The `_meta` of `session/new`, through which agents take options of their own.
 * @param firstPromptLead Sent ahead of the first prompt's own content, for an agent that takes no system prompt.
 */
public data class ConversationOpening(
    val meta: JsonElement? = null,
    val firstPromptLead: String? = null,
)
