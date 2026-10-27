/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp.clients

internal sealed interface AiClientStatus {

    data object Checking : AiClientStatus

    /**
     * The tool's program wasn't found under the command the settings name.
     */
    data object NotInstalled : AiClientStatus

    /**
     * The tool is installed, but a program it needs to reach Kubriko Creator isn't.
     */
    data class NeedsProgram(
        val program: AiConfigFileClient.RequiredProgram,
    ) : AiClientStatus

    data object NotConnected : AiClientStatus

    data object Connected : AiClientStatus

    /**
     * Connecting or disconnecting failed; [output] is what the tool printed about it.
     */
    data class Failed(
        val output: String,
    ) : AiClientStatus
}
