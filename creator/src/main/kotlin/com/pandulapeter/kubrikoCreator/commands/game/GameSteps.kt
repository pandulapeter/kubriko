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

import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.project.toJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_game_command_failed
import kubriko.creator.generated.resources.error_game_no_answer
import kubriko.creator.generated.resources.error_game_not_running
import kubriko.creator.generated.resources.error_game_unsupported
import java.awt.event.KeyEvent

/**
 * The outcome of a tool that did [this] with the running game: the console lines it produced (and the screenshot it
 * took), or why it couldn't.
 */
internal suspend fun GameStep.toOutcome() = when (this) {
    is GameStep.Done -> CommandOutcome.Success(result = buildJsonObject { put("console", console.toJsonArray()) }, image = png)
    GameStep.NotPlaying -> CommandOutcome.Rejected(Rejection(Res.string.error_game_not_running))
    GameStep.Unsupported -> CommandOutcome.Rejected(Rejection(Res.string.error_game_unsupported))
    GameStep.NoAnswer -> CommandOutcome.Rejected(Rejection(Res.string.error_game_no_answer))
    is GameStep.Failed -> CommandOutcome.Rejected(Rejection(Res.string.error_game_command_failed, message))
}

/**
 * Whether [name] is a key the running game can be sent: a `java.awt.event.KeyEvent` key name without `VK_`.
 */
internal fun isKeyName(name: String) = name.isNotEmpty() && try {
    KeyEvent::class.java.getField("VK_${name.uppercase()}")
    true
} catch (_: NoSuchFieldException) {
    false
}
