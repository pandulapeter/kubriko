/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.sprite_editor_flip_horizontally
import kubriko.creator.generated.resources.sprite_editor_flip_vertically
import kubriko.creator.generated.resources.sprite_editor_resize_canvas
import kubriko.creator.generated.resources.sprite_editor_rotate_clockwise
import kubriko.creator.generated.resources.sprite_editor_rotate_counterclockwise
import kubriko.creator.generated.resources.sprite_editor_scale
import kubriko.creator.generated.resources.sprite_editor_shift
import org.jetbrains.compose.resources.StringResource

/**
 * The entries of the sprite editor's Image menu: each an operation on a whole frame, or each of them; those that take
 * numbers ([isAsking]) open a dialog for them first ([SpriteOperationDialog]).
 */
internal enum class SpriteImageAction(
    val title: StringResource,
    val operation: EditSprite.Operation? = null,
) {
    FLIP_HORIZONTALLY(Res.string.sprite_editor_flip_horizontally, EditSprite.Operation.Flip(isVertical = false)),
    FLIP_VERTICALLY(Res.string.sprite_editor_flip_vertically, EditSprite.Operation.Flip(isVertical = true)),
    ROTATE_CLOCKWISE(Res.string.sprite_editor_rotate_clockwise, EditSprite.Operation.Rotate(isClockwise = true)),
    ROTATE_COUNTERCLOCKWISE(Res.string.sprite_editor_rotate_counterclockwise, EditSprite.Operation.Rotate(isClockwise = false)),
    SHIFT(Res.string.sprite_editor_shift),
    RESIZE_CANVAS(Res.string.sprite_editor_resize_canvas),
    SCALE(Res.string.sprite_editor_scale),
    ;

    val isAsking: Boolean get() = operation == null
}
