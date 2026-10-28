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
 * What a pause Block does to the game.
 */
@Serializable
internal enum class PauseAction {
    @SerialName("pause")
    PAUSE,

    @SerialName("resume")
    RESUME,

    @SerialName("toggle")
    TOGGLE,
}
