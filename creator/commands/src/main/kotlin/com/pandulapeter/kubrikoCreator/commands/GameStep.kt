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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry

/**
 * What a [ProjectRunner] did with the running game.
 */
public sealed interface GameStep {

    /**
     * @param console What the console showed meanwhile.
     * @param png The screenshot, for [ProjectRunner.captureScreen].
     */
    public class Done(
        public val console: List<ConsoleEntry>,
        public val png: ByteArray? = null,
    ) : GameStep

    public data object NotPlaying : GameStep

    /**
     * The game runs on a Kubriko version that can't be played from Kubriko Creator's tools.
     */
    public data object Unsupported : GameStep

    public data object NoAnswer : GameStep

    /**
     * The game runs on [target], which only a desktop game's tools reach.
     */
    public data class Elsewhere(
        val target: PlayTarget,
    ) : GameStep

    /**
     * @param message What the game said about it, in English.
     */
    public data class Failed(
        val message: String,
    ) : GameStep
}
