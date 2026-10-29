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
 * How an ACP agent is started: a program speaking the Agent Client Protocol over its standard input and output.
 *
 * @param environment Variables added to Kubriko Creator's own environment.
 */
internal data class AgentCommand(
    val program: String,
    val arguments: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
)
