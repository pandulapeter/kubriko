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
 * An agent's archive being downloaded on its first start.
 *
 * @param totalBytes The archive's size, or null if the server didn't say.
 */
public data class AgentDownload(
    val agentName: String,
    val downloadedBytes: Long,
    val totalBytes: Long?,
)
