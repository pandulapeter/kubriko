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
 * Builds and plays the project for action commands, reporting what the console showed meanwhile.
 */
internal interface ProjectRunner {

    /**
     * Compiles the project's desktop module.
     */
    suspend fun build(): RunResult

    /**
     * Starts the game, lets it run for [durationInMilliseconds] once it is up, and stops it.
     *
     * @return Whether the game started, and what the console showed meanwhile.
     */
    suspend fun play(durationInMilliseconds: Long): RunResult

    /**
     * Builds and starts the game, waits until it is up, and leaves it running for [pressKeys], [wait], and
     * [captureScreen], until [stopGame].
     *
     * @return Whether the game started, and what the console showed meanwhile.
     */
    suspend fun startGame(): RunResult

    /**
     * @return Whether a game was running, and what the console showed while it stopped.
     */
    suspend fun stopGame(): RunResult

    /**
     * Holds [keys] (named as in `java.awt.event.KeyEvent`, without `VK_`) down together in the running game for
     * [durationInMilliseconds], then releases them.
     */
    suspend fun pressKeys(keys: List<String>, durationInMilliseconds: Long): GameStep

    /**
     * Lets the running game run for [durationInMilliseconds].
     */
    suspend fun wait(durationInMilliseconds: Long): GameStep

    /**
     * Captures what the running game shows, as a PNG.
     */
    suspend fun captureScreen(): GameStep

    /**
     * @param isSuccessful Whether the build succeeded, or the game started.
     * @param console What the console showed meanwhile.
     */
    data class RunResult(
        val isSuccessful: Boolean,
        val console: List<ConsoleEntry>,
    )
}
