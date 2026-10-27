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

/**
 * A [WriteCommand] together with its input, ready to be validated and applied.
 */
internal data class Invocation<I>(
    val command: WriteCommand<I>,
    val input: I,
) {
    val title get() = command.title

    val mergeKey get() = command.mergeKey(input)

    fun validate(context: CommandContext) = command.validate(context, input)

    suspend fun apply(context: CommandContext) = command.apply(context, input)
}
