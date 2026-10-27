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

import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry

/**
 * What a [ProjectRunner] did with the running game.
 */
internal sealed interface GameStep {

    /**
     * @param console What the console showed meanwhile.
     * @param png The screenshot, for [ProjectRunner.captureScreen].
     */
    class Done(
        val console: List<ConsoleEntry>,
        val png: ByteArray? = null,
    ) : GameStep

    data object NotPlaying : GameStep

    /**
     * The game runs on a Kubriko version that can't be played from Kubriko Creator's tools.
     */
    data object Unsupported : GameStep

    data object NoAnswer : GameStep

    /**
     * @param message What the game said about it, in English.
     */
    data class Failed(
        val message: String,
    ) : GameStep
}
