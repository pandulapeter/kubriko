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

import kotlin.random.Random

/**
 * Rolls whether an alien attempts a shot during a tick, keeping the old one-in-80-per-60-Hz-tick average at any tick rate.
 */
internal fun shouldAlienAttemptShot(
    deltaTimeInMilliseconds: Int,
    random: Random = Random,
) = random.nextFloat() < deltaTimeInMilliseconds / AVERAGE_ALIEN_SHOT_INTERVAL_IN_MILLISECONDS

private const val AVERAGE_ALIEN_SHOT_INTERVAL_IN_MILLISECONDS = 1333f
