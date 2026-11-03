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
 * How much of its context a conversation uses, and what it cost so far, as the agent last reported.
 *
 * @param usedTokens The tokens the conversation's context holds.
 * @param contextTokens The most the context can hold.
 * @param cost What the conversation cost so far, in [currency], where the agent says.
 */
public data class ConversationUsage(
    val usedTokens: Long,
    val contextTokens: Long,
    val cost: Double? = null,
    val currency: String? = null,
)
