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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object ListPlayTargets : ReadCommand<NoInput> {
    override val name: String = "list_play_targets"
    override val description: String = "Lists where the game can be played now, by the ids play and start_game take as target, and the one the user's Play button uses."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    /**
     * Where the game can be played is this computer's, and reading it waits for no change.
     */
    override val readsProject: Boolean = false

    override fun target(context: CommandContext, input: NoInput): CommandTarget? = CommandTarget.Game

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome {
        val targets = context.runner.playTargets()
        return CommandOutcome.Success(
            buildJsonObject {
                putJsonArray("targets") { targets.available.forEach { add(Json.encodeToJsonElement(PlayTarget.serializer(), it)) } }
                put("chosenByUser", targets.chosen.id)
            },
        )
    }
}
