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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.GameStep
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.project.toJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.event.KeyEvent

/**
 * The outcome of a tool that did [this] with the running game: the console lines it produced (and the screenshot it
 * took, with a [note] on it), or why it couldn't.
 */
internal suspend fun GameStep.toOutcome(note: String? = null) = when (this) {
    is GameStep.Done -> CommandOutcome.Success(
        result = buildJsonObject {
            put("console", console.toJsonArray())
            note?.let { put("note", it) }
        },
        image = png,
    )
    else -> CommandOutcome.Rejected(checkNotNull(rejection()))
}

/**
 * Why the running game couldn't do what was asked, or null if it did.
 */
internal fun GameStep.rejection(): Rejection? = when (this) {
    is GameStep.Done -> null
    GameStep.NotPlaying -> Rejection(CommandProblem.GAME_NOT_RUNNING)
    GameStep.Unsupported -> Rejection(CommandProblem.GAME_UNSUPPORTED)
    GameStep.NoAnswer -> Rejection(CommandProblem.GAME_NO_ANSWER)
    is GameStep.Elsewhere -> Rejection(CommandProblem.GAME_NOT_ON_DESKTOP, target.id)
    is GameStep.Failed -> Rejection(CommandProblem.GAME_COMMAND_FAILED, message)
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

/**
 * Why the running game can't be sent the key [name], naming the keys called similarly.
 */
internal fun unknownKeyName(name: String) = SimilarNames.rejection(
    CommandProblem.GAME_KEY_UNKNOWN,
    CommandProblem.GAME_KEY_UNKNOWN_SIMILAR,
    name,
    KeyEvent::class.java.fields.map { it.name }.filter { it.startsWith("VK_") }.map { it.removePrefix("VK_") },
    name,
)
