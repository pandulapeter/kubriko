/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which keys a Move with keys Block steers with.
 */
@Serializable
public enum class SteeringKeys(public val usesArrows: Boolean, public val usesWasd: Boolean) {
    @SerialName("arrows")
    ARROWS(usesArrows = true, usesWasd = false),

    @SerialName("wasd")
    WASD(usesArrows = false, usesWasd = true),

    @SerialName("arrowsAndWasd")
    ARROWS_AND_WASD(usesArrows = true, usesWasd = true),
}
