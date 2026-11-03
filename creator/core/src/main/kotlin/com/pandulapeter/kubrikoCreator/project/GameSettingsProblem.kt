/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a value of the game's settings can't be used.
 */
public enum class GameSettingsProblem(override val english: String) : DomainText {
    FIRST_SCENE_INVALID("\"%1\$s\" isn't a Scene name: folder names and a name separated by slashes, like levels/1-1."),
    VIEWPORT_SIZE_INVALID("The viewport's width and height are numbers from 1 to %1\$s."),
    FRAME_RATE_INVALID("The frame rate is a whole number of updates per second from 1 to %1\$s, or 0 for every frame the display shows."),
}
