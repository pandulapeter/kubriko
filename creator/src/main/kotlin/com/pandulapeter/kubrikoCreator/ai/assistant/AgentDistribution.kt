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

    /**
     * A Python package that the user's `uvx` (from uv) runs.
     */
    data class Uvx(
        val packageName: String,
        val arguments: List<String> = emptyList(),
        val environment: Map<String, String> = emptyMap(),
    ) : AgentDistribution

    /**
     * An archive of this platform's build, unpacked under Kubriko Creator's tools folder ([BinaryAgentInstaller]).
     *
     * @param url Where the archive is downloaded from: a `.zip`, a `.tar.gz`, a `.tar.bz2`, or the program itself.
     * @param command The program in the archive, relative to its root (`./agy_acp_server.par`).
     * @param sha256 The archive's checksum, which it is checked against where the registry gives one.
     */
    data class Binary(
        val url: String,
        val command: String,
        val arguments: List<String> = emptyList(),
        val environment: Map<String, String> = emptyMap(),
        val sha256: String? = null,
    ) : AgentDistribution
}
