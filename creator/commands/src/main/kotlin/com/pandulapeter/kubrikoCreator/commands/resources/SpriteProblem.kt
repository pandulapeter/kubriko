/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * Why a sprite, a frame of it, its sheet, or the palette can't be made, read, or drawn on as asked.
 */
public enum class SpriteProblem(override val english: String) : DomainText {
    PIXELS_NONE("Give pixels, rows, or both: there's nothing to draw."),
    PIXELS_PALETTE_KEY("Each palette entry is one letter, and . can't be one: it leaves a pixel as it is."),
    PIXELS_LETTER_UNKNOWN("The palette has no color for these letters of rows: %1\$s."),
    PIXELS_OUTSIDE_FRAME("The pixel at %1\$s is outside the frame, which is %2\$s by %3\$s pixels, from 0, 0 at its top left."),
    PALETTE_TOO_LARGE("A palette holds at most %1\$s colors."),
    PALETTE_NOT_WRITTEN("Kubriko Creator couldn't save the palette, so it stays as it was: %1\$s"),
    FRAMES_NONE("Give at least one image to make the frames of."),
    FRAMES_DIFFER_IN_SIZE("%1\$s isn't %2\$s by %3\$s pixels as the first image is: the frames of a sprite are all of one size."),
    SPRITE_SIZE_INVALID("A sprite's width and height are whole numbers of pixels from 1 to %1\$s."),
    SPRITE_SCALE_INVALID("A sprite is scaled up by a whole number from 1 to %1\$s."),
    SPRITE_FRAME_SIZE_ALONE("One frame of a sprite cut into frames can't change size on its own, since its frames are all of one size: change them all."),
    SPRITE_PATH_INVALID("%1\$s can't be a new sprite: drawable/, then a name of lowercase letters, digits, and dashes, then .png, like drawable/ship.png."),
    SPRITE_TOO_LARGE_FOR_ROWS("Only an image of at most %1\$s by %1\$s pixels and %2\$s colors can be read as rows: read a frame, or leave asRows out."),
    SPRITE_UNREADABLE("The image of %1\$s can't be read, so it can't be changed: it isn't a PNG, JPEG, or WebP image, or it is gone."),
    SPRITE_NOT_WRITTEN("Kubriko Creator couldn't save %1\$s, so it stays as it was: %2\$s"),
    SPRITE_FRAME_LAST("A sprite keeps at least one frame: draw over it instead, or delete the sprite."),
    SPRITE_FRAME_MISSING("%1\$s has no frame %2\$s: its frames go from 0 to %3\$s."),
    SPRITE_SHEET_NOT_WRITTEN("Kubriko Creator couldn't save how %1\$s is cut into frames, so it stays as it was: %2\$s"),
    SPRITE_SHEET_DOES_NOT_FIT("%1\$s is %2\$s by %3\$s pixels, which the frames don't fit: their rows take more room than it has."),
}
