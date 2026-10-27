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
 * A command that changes the project. It is validated before it is applied, and applying it returns the command that
 * reverts it, which is what undo and redo run.
 */
internal interface WriteCommand<I> : EditorCommand<I> {

    /**
     * How the user interface names the change, in the titles of undo and redo.
     */
    val title: StringResource

    /**
     * Whether the change throws work away for good (it can't be undone), so that AI clients ask before making it.
     */
    val isDestructive: Boolean get() = false

    /**
     * Why [input] can't be applied to the project as it is now, or null if it can.
     */
    fun validate(context: CommandContext, input: I): Rejection?

    /**
     * Makes the change [input] describes, which [validate] has accepted.
     */
    suspend fun apply(context: CommandContext, input: I): Applied

    /**
     * Consecutive changes with the same non-null key are undone together, so that typing into one field is a single step.
     */
    fun mergeKey(input: I): Any? = null
}
