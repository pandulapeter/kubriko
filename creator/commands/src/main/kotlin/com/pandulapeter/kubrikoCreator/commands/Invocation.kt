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

import com.pandulapeter.kubrikoCreator.context.RelationSubject

/**
 * A [WriteCommand] together with its input, ready to be validated and applied.
 *
 * @param isRestoring Whether it undoes or redoes a change, restoring what was there ([WriteCommand.validateRestoring]),
 *   rather than being a new one.
 */
public data class Invocation<I>(
    val command: WriteCommand<I>,
    val input: I,
    val isRestoring: Boolean = false,
) {
    internal val commandName get() = command.commandName(input)

    internal fun target(context: CommandContext) = command.target(context, input)

    internal fun targets(context: CommandContext) = command.targets(context, input)

    /**
     * Whether [next], made right after this, is undone together with it ([WriteCommand.mergesWith]).
     */
    internal fun mergesWith(next: Invocation<*>): Boolean = next.command == command && command.mergesWith(input, castInput(next))

    /**
     * What reverts this change together with the one reverted by [latest], made after it ([WriteCommand.mergeReverts]).
     */
    internal fun mergedWith(latest: Invocation<*>): Invocation<I> = copy(input = command.mergeReverts(input, castInput(latest)))

    @Suppress("UNCHECKED_CAST")
    private fun castInput(other: Invocation<*>) = other.input as I

    internal fun validate(context: CommandContext) = if (isRestoring) command.validateRestoring(context, input) else command.validate(context, input)

    internal suspend fun apply(context: CommandContext) = command.apply(context, input)

    /**
     * What this deletes, if it is a delete ([DeletingCommand]).
     */
    @Suppress("UNCHECKED_CAST")
    public val deletedSubject: RelationSubject? get() = (command as? DeletingCommand<I>)?.subject(input)
}
