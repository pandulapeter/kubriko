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
 * The agent couldn't be started, or ended before it answered.
 *
 * @param exitCode The process's exit code, or null if it never started.
 * @param errorOutput The last lines it wrote to its standard error.
 */
internal class AgentStartException(
    val exitCode: Int?,
    val errorOutput: List<String>,
    cause: Throwable? = null,
) : IllegalStateException("The agent couldn't be started" + (exitCode?.let { " (exit code $it)" } ?: "") + ".", cause)
