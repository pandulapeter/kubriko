/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the game is played from this computer, which is this computer's alone (`.kubriko/local/` is git-ignored).
 *
 * @param target The ID of the place Play plays the game, or null for the desktop.
 */
@Serializable
internal data class LocalPlaySettings(
    @SerialName("target") val target: String? = null,
)
