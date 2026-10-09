/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlienFireChanceTest {

    private fun attemptsOver(simulatedTimeInMilliseconds: Int, deltaTimeInMilliseconds: Int, random: Random): Int {
        var attempts = 0
        repeat(simulatedTimeInMilliseconds / deltaTimeInMilliseconds) {
            if (shouldAlienAttemptShot(deltaTimeInMilliseconds, random)) {
                attempts++
            }
        }
        return attempts
    }

    @Test
    fun attemptsPerSecondDoNotDependOnTheTickRate() {
        val expectedAttempts = SIMULATED_TIME_IN_MILLISECONDS / 1333f
        listOf(
            attemptsOver(SIMULATED_TIME_IN_MILLISECONDS, 8, Random(42)),
            attemptsOver(SIMULATED_TIME_IN_MILLISECONDS, 33, Random(42)),
        ).forEach { attempts ->
            assertTrue(abs(attempts - expectedAttempts) <= expectedAttempts * 0.03f, "$attempts attempts, expected about $expectedAttempts")
        }
    }

    @Test
    fun zeroDeltaNeverAttempts() {
        val random = Random(1)
        repeat(1_000) {
            assertFalse(shouldAlienAttemptShot(0, random))
        }
    }

    private companion object {
        const val SIMULATED_TIME_IN_MILLISECONDS = 10_000_000
    }
}
