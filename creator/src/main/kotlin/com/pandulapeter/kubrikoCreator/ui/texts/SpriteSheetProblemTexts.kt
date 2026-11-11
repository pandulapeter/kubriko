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

import com.pandulapeter.kubrikoCreator.sprites.SpriteSheetProblem
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_sprite_sheet_frame_count
import kubriko.creator.generated.resources.error_sprite_sheet_frame_size
import kubriko.creator.generated.resources.error_sprite_sheet_frames_per_second
import kubriko.creator.generated.resources.error_sprite_sheet_larger_than_image
import kubriko.creator.generated.resources.error_sprite_sheet_origin
import org.jetbrains.compose.resources.StringResource

/**
 * The string the user is shown for this [SpriteSheetProblem].
 */
internal fun SpriteSheetProblem.toResource(): StringResource = when (this) {
    SpriteSheetProblem.FRAME_SIZE -> Res.string.error_sprite_sheet_frame_size
    SpriteSheetProblem.FRAME_COUNT -> Res.string.error_sprite_sheet_frame_count
    SpriteSheetProblem.FRAMES_PER_SECOND -> Res.string.error_sprite_sheet_frames_per_second
    SpriteSheetProblem.ORIGIN -> Res.string.error_sprite_sheet_origin
    SpriteSheetProblem.LARGER_THAN_IMAGE -> Res.string.error_sprite_sheet_larger_than_image
}
