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
 * Builds and plays the project for action commands, reporting what the console showed meanwhile.
 */
public interface ProjectRunner {

    /**
     * Compiles the project's desktop module.
     */
    public suspend fun build(): RunResult

    /**
     * Looks for where the game can be played now.
     */
    public suspend fun playTargets(): PlayTargets

    /**
     * Starts the game on [target], lets it run for [durationInMilliseconds] once it is up, and stops it.
     *
     * @return Whether the game started, and what the console showed meanwhile.
     */
    public suspend fun play(durationInMilliseconds: Long, target: PlayTarget = PlayTarget.DESKTOP): RunResult

    /**
     * Builds and starts the game on [target], waits until it is up, and leaves it running for [pressKeys], [wait], and
     * [captureScreen], until [stopGame].
     *
     * @param firstScene The Scene the game starts in instead of its first one.
     * @return Whether the game started, and what the console showed meanwhile.
     */
    public suspend fun startGame(firstScene: String? = null, target: PlayTarget = PlayTarget.DESKTOP): RunResult

    /**
     * @return Whether a game was running, and what the console showed while it stopped.
     */
    public suspend fun stopGame(): RunResult

    /**
     * Holds [keys] (named as in `java.awt.event.KeyEvent`, without `VK_`) down together in the running game for
     * [durationInMilliseconds], then releases them.
     */
    public suspend fun pressKeys(keys: List<String>, durationInMilliseconds: Long): GameStep

    /**
     * Lets the running game run for [durationInMilliseconds].
     */
    public suspend fun wait(durationInMilliseconds: Long): GameStep

    /**
     * Captures what the running game shows, as a PNG.
     */
    public suspend fun captureScreen(): GameStep

    /**
     * @param isSuccessful Whether the build succeeded, or the game started.
     * @param console What the console showed meanwhile.
     */
    public data class RunResult(
        val isSuccessful: Boolean,
        val console: List<ConsoleEntry>,
    )
}
