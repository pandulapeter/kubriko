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
 * A command that only reports something about the project, so it never enters the undo history.
 */
public interface ReadCommand<I> : EditorCommand<I> {

    /**
     * Whether it reads the open project, and so waits for a change in progress; false for one that reads only Kubriko
     * Creator's own knowledge.
     */
    public val readsProject: Boolean get() = true

    /**
     * Reports what [input] asks for. Runs while no change is being made (when [readsProject]), so it must not make one.
     */
    public suspend fun read(context: CommandContext, input: I): CommandOutcome
}
