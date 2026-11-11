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

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubrikoCreator.sprites.pixels.CanvasAnchor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.canvas_anchor_bottom
import kubriko.creator.generated.resources.canvas_anchor_bottom_left
import kubriko.creator.generated.resources.canvas_anchor_bottom_right
import kubriko.creator.generated.resources.canvas_anchor_center
import kubriko.creator.generated.resources.canvas_anchor_left
import kubriko.creator.generated.resources.canvas_anchor_right
import kubriko.creator.generated.resources.canvas_anchor_top
import kubriko.creator.generated.resources.canvas_anchor_top_left
import kubriko.creator.generated.resources.canvas_anchor_top_right
import kubriko.creator.generated.resources.ic_edit
import kubriko.creator.generated.resources.ic_ellipse
import kubriko.creator.generated.resources.ic_eraser
import kubriko.creator.generated.resources.ic_fill
import kubriko.creator.generated.resources.ic_line
import kubriko.creator.generated.resources.ic_picker
import kubriko.creator.generated.resources.ic_rectangle
import kubriko.creator.generated.resources.ic_select
import kubriko.creator.generated.resources.sprite_editor_tool_ellipse
import kubriko.creator.generated.resources.sprite_editor_tool_eraser
import kubriko.creator.generated.resources.sprite_editor_tool_fill
import kubriko.creator.generated.resources.sprite_editor_tool_line
import kubriko.creator.generated.resources.sprite_editor_tool_pencil
import kubriko.creator.generated.resources.sprite_editor_tool_picker
import kubriko.creator.generated.resources.sprite_editor_tool_rectangle
import kubriko.creator.generated.resources.sprite_editor_tool_select
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal val SpriteTool.title: StringResource
    get() = when (this) {
        SpriteTool.PENCIL -> Res.string.sprite_editor_tool_pencil
        SpriteTool.ERASER -> Res.string.sprite_editor_tool_eraser
        SpriteTool.LINE -> Res.string.sprite_editor_tool_line
        SpriteTool.RECTANGLE -> Res.string.sprite_editor_tool_rectangle
        SpriteTool.ELLIPSE -> Res.string.sprite_editor_tool_ellipse
        SpriteTool.FILL -> Res.string.sprite_editor_tool_fill
        SpriteTool.PICKER -> Res.string.sprite_editor_tool_picker
        SpriteTool.SELECT -> Res.string.sprite_editor_tool_select
    }

internal val SpriteTool.icon: DrawableResource
    get() = when (this) {
        SpriteTool.PENCIL -> Res.drawable.ic_edit
        SpriteTool.ERASER -> Res.drawable.ic_eraser
        SpriteTool.LINE -> Res.drawable.ic_line
        SpriteTool.RECTANGLE -> Res.drawable.ic_rectangle
        SpriteTool.ELLIPSE -> Res.drawable.ic_ellipse
        SpriteTool.FILL -> Res.drawable.ic_fill
        SpriteTool.PICKER -> Res.drawable.ic_picker
        SpriteTool.SELECT -> Res.drawable.ic_select
    }

/**
 * The key that chooses the tool, as its title says.
 */
internal val SpriteTool.key: Key
    get() = when (this) {
        SpriteTool.PENCIL -> Key.B
        SpriteTool.ERASER -> Key.E
        SpriteTool.LINE -> Key.L
        SpriteTool.RECTANGLE -> Key.R
        SpriteTool.ELLIPSE -> Key.O
        SpriteTool.FILL -> Key.G
        SpriteTool.PICKER -> Key.I
        SpriteTool.SELECT -> Key.S
    }

internal val CanvasAnchor.title: StringResource
    get() = when (this) {
        CanvasAnchor.TOP_LEFT -> Res.string.canvas_anchor_top_left
        CanvasAnchor.TOP -> Res.string.canvas_anchor_top
        CanvasAnchor.TOP_RIGHT -> Res.string.canvas_anchor_top_right
        CanvasAnchor.LEFT -> Res.string.canvas_anchor_left
        CanvasAnchor.CENTER -> Res.string.canvas_anchor_center
        CanvasAnchor.RIGHT -> Res.string.canvas_anchor_right
        CanvasAnchor.BOTTOM_LEFT -> Res.string.canvas_anchor_bottom_left
        CanvasAnchor.BOTTOM -> Res.string.canvas_anchor_bottom
        CanvasAnchor.BOTTOM_RIGHT -> Res.string.canvas_anchor_bottom_right
    }
