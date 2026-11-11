/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a [SpriteSheet] can't be played.
 */
public enum class SpriteSheetProblem(override val english: String) : DomainText {
    FRAME_SIZE("A frame's width and height are whole numbers of pixels from 1."),
    FRAME_COUNT("The number of frames, and of frames in a row, are whole numbers from 1."),
    FRAMES_PER_SECOND("The frames per second are a number above 0, at most 240."),
    ORIGIN("The origin is inside a frame: from 0 to its width across, and from 0 to its height down."),
    LARGER_THAN_IMAGE("The frames don't fit the image: their rows take more room than it has."),
}
