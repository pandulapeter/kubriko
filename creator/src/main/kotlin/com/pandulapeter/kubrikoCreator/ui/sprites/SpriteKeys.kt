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
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage

/**
 * What a key pressed in the sprite editor does on [frame], with Ctrl or Cmd held when [isShortcut]: copy, cut, and paste
 * (pasting puts down what floated before), Delete deleting what is selected or floats, Enter putting floating pixels down,
 * Escape letting go of both (pixels lifted but not put down stay where they were, as nothing was drawn yet), and a tool's
 * letter choosing it. Drawing goes through [draw]; returns whether the key did something, so the rest (undo and redo above
 * all) go on to the workspace.
 */
internal fun SpriteEditorState.onKey(key: Key, isShortcut: Boolean, frame: PixelImage, draw: (List<Pixel>) -> Unit): Boolean = when {
    isShortcut && key == Key.C -> copy(frame)
    isShortcut && key == Key.X -> copy(frame).also { if (it) draw(if (floating != null) dropFloating(frame) else clearedSelection(frame)) }
    isShortcut && key == Key.V -> {
        draw(putDown(frame))
        paste()
    }
    isShortcut -> false
    key == Key.Delete || key == Key.Backspace -> when {
        floating != null -> true.also { draw(dropFloating(frame)) }
        selection != null -> true.also { draw(clearedSelection(frame)) }
        else -> false
    }
    key == Key.Enter || key == Key.NumPadEnter -> (floating != null).also { if (it) draw(putDown(frame)) }
    key == Key.Escape -> (floating != null || selection != null).also {
        floating = null
        selection = null
    }
    else -> SpriteTool.entries.firstOrNull { it.key == key }?.also { tool = it } != null
}
