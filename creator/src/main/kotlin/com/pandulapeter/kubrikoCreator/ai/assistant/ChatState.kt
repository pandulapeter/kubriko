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

/**
 * Whether a conversation of the assistant panel can take prompts.
 */
internal sealed interface ChatState {

    /**
     * The agent is being started, which the first time includes `npx` downloading it.
     */
    data object Starting : ChatState

    data object Ready : ChatState

    /**
     * @param details The agent's last lines of standard error, or the error's own words.
     */
    data class Failed(
        val reason: FailureReason,
        val details: List<String> = emptyList(),
    ) : ChatState
}
