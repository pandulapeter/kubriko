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
 * A command that changes the project. It is validated before it is applied, and applying it returns the command that
 * reverts it, which is what undo and redo run.
 */
internal interface WriteCommand<I> : EditorCommand<I> {

    /**
     * Whether the change throws work away for good (it can't be undone), so that AI clients ask before making it.
     */
    val isDestructive: Boolean get() = false

    /**
     * The tool name the change [input] describes is recorded and named under: [name], unless it depends on the input.
     */
    fun commandName(input: I): String = name

    /**
     * Everything the command with [input] changes: its [target], unless it changes more than one thing.
     */
    fun targets(context: CommandContext, input: I): Set<CommandTarget> = setOfNotNull(target(context, input))

    /**
     * Why [input] can't be applied to the project as it is now, or null if it can.
     */
    fun validate(context: CommandContext, input: I): Rejection?

    /**
     * Why [input], made by undoing or redoing a change, can't restore what was there before. What was there may refer to
     * types, Scenes, or sounds deleted since, which is valid to keep though not to create, so a command whose input holds
     * such content checks only the structure here (that its type, Event, or Scene exists, and that names don't clash).
     */
    fun validateRestoring(context: CommandContext, input: I): Rejection? = validate(context, input)

    /**
     * Makes the change [input] describes, which [validate] has accepted.
     */
    suspend fun apply(context: CommandContext, input: I): Applied

    /**
     * Consecutive changes with the same non-null key are undone together, so that typing into one field is a single step.
     */
    fun mergeKey(input: I): Any? = null

    /**
     * Whether [next], made right after [previous], is undone together with it: by default, when both have the same
     * non-null [mergeKey].
     */
    fun mergesWith(previous: I, next: I): Boolean = mergeKey(previous)?.let { it == mergeKey(next) } == true

    /**
     * What reverts two changes undone together, given what reverts the earlier one ([first]) and the later one ([latest]).
     * By default [first], which is right for a change that addresses what it changes the same way before and after it.
     */
    fun mergeReverts(first: I, latest: I): I = first
}
