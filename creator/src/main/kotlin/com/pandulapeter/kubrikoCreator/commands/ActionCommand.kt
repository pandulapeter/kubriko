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
 * A command that does something with the project without changing it (building it, or playing the game), so it never
 * enters the undo history, and doesn't wait for or hold up changes.
 */
internal interface ActionCommand<I> : EditorCommand<I> {

    suspend fun run(context: CommandContext, input: I): CommandOutcome
}
