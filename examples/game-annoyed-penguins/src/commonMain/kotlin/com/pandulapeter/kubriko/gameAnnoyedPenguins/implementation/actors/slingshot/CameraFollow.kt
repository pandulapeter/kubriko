/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot

import kotlin.math.pow

/**
 * The fraction of the gap between the camera and its target closed in a tick: 2.5 % per 60 Hz tick, at any tick rate.
 */
internal fun cameraFollowFactor(deltaTimeInMilliseconds: Int) =
    1f - CAMERA_FOLLOW_RETENTION_PER_REFERENCE_TICK.pow(deltaTimeInMilliseconds / REFERENCE_TICK_IN_MILLISECONDS)

private const val CAMERA_FOLLOW_RETENTION_PER_REFERENCE_TICK = 0.975f
private const val REFERENCE_TICK_IN_MILLISECONDS = 1000f / 60
