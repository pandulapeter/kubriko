/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import org.jetbrains.compose.resources.StringResource

/**
 * One tool call of an AI client, as the editor shows the AI's work.
 *
 * @param target What the call reads or changes, if anything in particular.
 * @param rejection Why the call failed, once it has.
 */
internal data class AiStep(
    val id: Int,
    val title: StringResource,
    val target: CommandTarget?,
    val isRunning: Boolean,
    val rejection: Rejection? = null,
)
