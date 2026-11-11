/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.commands.resources.SpriteProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_frames_differ_in_size
import kubriko.creator.generated.resources.error_frames_none
import kubriko.creator.generated.resources.error_palette_not_written
import kubriko.creator.generated.resources.error_palette_too_large
import kubriko.creator.generated.resources.error_pixels_letter_unknown
import kubriko.creator.generated.resources.error_pixels_none
import kubriko.creator.generated.resources.error_pixels_outside_frame
import kubriko.creator.generated.resources.error_pixels_palette_key
import kubriko.creator.generated.resources.error_sprite_frame_missing
import kubriko.creator.generated.resources.error_sprite_frame_size_alone
import kubriko.creator.generated.resources.error_sprite_not_written
import kubriko.creator.generated.resources.error_sprite_path_invalid
import kubriko.creator.generated.resources.error_sprite_scale_invalid
import kubriko.creator.generated.resources.error_sprite_sheet_does_not_fit
import kubriko.creator.generated.resources.error_sprite_sheet_not_written
import kubriko.creator.generated.resources.error_sprite_size_invalid
import kubriko.creator.generated.resources.error_sprite_too_large_for_rows
import kubriko.creator.generated.resources.error_sprite_unreadable
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [SpriteProblem].
 */
internal fun SpriteProblem.toResource(): StringResource = when (this) {
    SpriteProblem.FRAMES_NONE -> Res.string.error_frames_none
    SpriteProblem.FRAMES_DIFFER_IN_SIZE -> Res.string.error_frames_differ_in_size
    SpriteProblem.PIXELS_NONE -> Res.string.error_pixels_none
    SpriteProblem.PIXELS_PALETTE_KEY -> Res.string.error_pixels_palette_key
    SpriteProblem.PIXELS_LETTER_UNKNOWN -> Res.string.error_pixels_letter_unknown
    SpriteProblem.PIXELS_OUTSIDE_FRAME -> Res.string.error_pixels_outside_frame
    SpriteProblem.PALETTE_TOO_LARGE -> Res.string.error_palette_too_large
    SpriteProblem.PALETTE_NOT_WRITTEN -> Res.string.error_palette_not_written
    SpriteProblem.SPRITE_SIZE_INVALID -> Res.string.error_sprite_size_invalid
    SpriteProblem.SPRITE_SCALE_INVALID -> Res.string.error_sprite_scale_invalid
    SpriteProblem.SPRITE_FRAME_SIZE_ALONE -> Res.string.error_sprite_frame_size_alone
    SpriteProblem.SPRITE_PATH_INVALID -> Res.string.error_sprite_path_invalid
    SpriteProblem.SPRITE_TOO_LARGE_FOR_ROWS -> Res.string.error_sprite_too_large_for_rows
    SpriteProblem.SPRITE_UNREADABLE -> Res.string.error_sprite_unreadable
    SpriteProblem.SPRITE_NOT_WRITTEN -> Res.string.error_sprite_not_written
    SpriteProblem.SPRITE_FRAME_MISSING -> Res.string.error_sprite_frame_missing
    SpriteProblem.SPRITE_SHEET_NOT_WRITTEN -> Res.string.error_sprite_sheet_not_written
    SpriteProblem.SPRITE_SHEET_DOES_NOT_FIT -> Res.string.error_sprite_sheet_does_not_fit
}
