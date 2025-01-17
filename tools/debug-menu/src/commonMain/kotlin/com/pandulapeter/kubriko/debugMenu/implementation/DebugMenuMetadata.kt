/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

internal data class DebugMenuMetadata(
    val kubrikoInstanceName: String = "",
    val fps: Float = 0f,
    val totalActorCount: Int = 0,
    val visibleActorWithinViewportCount: Int = 0,
    val playTimeInSeconds: Long = 0,
    val isDebugOverlayEnabled: Boolean = false,
)