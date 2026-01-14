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
 * TODO: Documentation
 */
class Timer(
    val timeInMilliseconds: Long,
    val shouldTriggerMultipleTimes: Boolean = false,
    val onDone: () -> Unit,
) {
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

    fun update(deltaTimeInMilliseconds: Int) {
        if (remainingTimeInMilliseconds > 0 || shouldTriggerMultipleTimes) {
            remainingTimeInMilliseconds -= deltaTimeInMilliseconds
        }
    }
}