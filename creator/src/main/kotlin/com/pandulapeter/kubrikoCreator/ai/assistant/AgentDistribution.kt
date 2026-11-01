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
 * How an agent of the ACP Registry is started.
 */
internal sealed interface AgentDistribution {

    /**
     * An npm package that the user's `npx` runs.
     *
     * @param environment Variables the registry asks the agent to be started with.
     */
    data class Npx(
        val packageName: String,
        val arguments: List<String> = emptyList(),
        val environment: Map<String, String> = emptyMap(),
    ) : AgentDistribution
}
