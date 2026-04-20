/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

/**
 * A utility class for handling time-based events.
 *
 * @param timeInMilliseconds The duration of the timer.
 * @param shouldTriggerMultipleTimes Whether the timer should restart automatically after finishing.
 * @param onDone Callback invoked when the timer reaches zero.
 */
class Timer(
    val timeInMilliseconds: Long,
    val shouldTriggerMultipleTimes: Boolean = false,
    val onDone: () -> Unit,
) {
    /**
     * The time remaining until the timer finishes.
     */
    var remainingTimeInMilliseconds = timeInMilliseconds
        private set(value) {
            field = if (value > 0) {
                value
            } else {
                onDone()
                if (shouldTriggerMultipleTimes) {
                    timeInMilliseconds
                } else {
                    0
                }
            }
        }

    /**
     * Updates the timer. This should be called on every frame of the game loop.
     *
     * @param deltaTimeInMilliseconds The time elapsed since the last update.
     */
    fun update(deltaTimeInMilliseconds: Int) {
        if (remainingTimeInMilliseconds > 0 || shouldTriggerMultipleTimes) {
            remainingTimeInMilliseconds -= deltaTimeInMilliseconds
        }
    }
}