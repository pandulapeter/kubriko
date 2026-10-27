/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.game

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.NoInput

internal object TakeScreenshot : ActionCommand<NoInput> {
    override val name = "take_screenshot"
    override val description = "Returns an image of what the game start_game started shows right now, at most 1280 pixels wide, with what the " +
        "console showed meanwhile."
    override val inputSerializer = NoInput.serializer()

    override suspend fun run(context: CommandContext, input: NoInput) = context.runner.captureScreen().toOutcome()
}
