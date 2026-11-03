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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection

/**
 * What a tool that plays the game found for the place its input named.
 */
internal sealed interface PlayTargetLookup {
    data class Found(val target: PlayTarget) : PlayTargetLookup

    data class Missing(val outcome: CommandOutcome.Rejected) : PlayTargetLookup

    companion object {
        /**
         * The place [id] names among those the game can be played on now (the desktop when it is null), or why it can't be
         * played there, naming the places it can.
         */
        suspend fun of(context: CommandContext, id: String?): PlayTargetLookup {
            if (id == null || id == PlayTarget.DESKTOP.id) return Found(PlayTarget.DESKTOP)
            val available = context.runner.playTargets().available
            return available.firstOrNull { it.id == id }?.let(::Found)
                ?: Missing(CommandOutcome.Rejected(Rejection(CommandProblem.PLAY_TARGET_MISSING, id, available.joinToString { it.id })))
        }
    }
}
