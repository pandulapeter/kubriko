/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation

/**
 * What the conversation menu offers: the conversations open now, then earlier ones that can be continued.
 */
internal sealed interface ConversationMenuItem {
    data class Open(
        val chat: AssistantChat,
    ) : ConversationMenuItem

    data class Earlier(
        val conversation: SavedConversation,
    ) : ConversationMenuItem
}
