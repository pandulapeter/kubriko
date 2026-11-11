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

import androidx.compose.ui.unit.IntOffset
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage

/**
 * One press on the sprite editor's canvas, from where it went down to where it is let go, doing what the tool does:
 * kept apart from the pointer handling so tests can play one without a window.
 */
internal sealed interface SpriteGesture {
    fun moveTo(pixel: IntOffset)

    /**
     * Ends the gesture, drawing through [draw] what it made, if anything.
     */
    fun finish(draw: (List<Pixel>) -> Unit)

    private class Drawing(private val state: SpriteEditorState) : SpriteGesture {
        override fun moveTo(pixel: IntOffset) = state.continueStroke(pixel)

        override fun finish(draw: (List<Pixel>) -> Unit) = draw(state.finishStroke())
    }

    private class Picking(private val state: SpriteEditorState, private val frame: PixelImage, private val isSecondary: Boolean) : SpriteGesture {
        override fun moveTo(pixel: IntOffset) = state.pickColor(frame, pixel, isSecondary)

        override fun finish(draw: (List<Pixel>) -> Unit) = Unit
    }

    private class Selecting(private val state: SpriteEditorState, private val frame: PixelImage, private val start: IntOffset) : SpriteGesture {
        override fun moveTo(pixel: IntOffset) {
            state.selection = selectionOf(start, pixel, frame)
        }

        override fun finish(draw: (List<Pixel>) -> Unit) = Unit
    }

    private class Moving(private val state: SpriteEditorState, private val start: IntOffset) : SpriteGesture {
        private val startPosition = state.floating?.position ?: IntOffset.Zero

        override fun moveTo(pixel: IntOffset) {
            state.floating = state.floating?.copy(position = startPosition + (pixel - start))
        }

        override fun finish(draw: (List<Pixel>) -> Unit) = Unit
    }

    companion object {
        /**
         * The gesture a press at [pixel] of [frame] starts, with the right button when [isSecondary]. A press with the
         * selection tool outside the floating pixels puts them down first, through [draw].
         */
        fun start(state: SpriteEditorState, frame: PixelImage, pixel: IntOffset, isSecondary: Boolean, draw: (List<Pixel>) -> Unit): SpriteGesture = when {
            state.tool == SpriteTool.PICKER -> Picking(state, frame, isSecondary).also { it.moveTo(pixel) }
            state.tool != SpriteTool.SELECT -> {
                state.startStroke(frame, pixel, isSecondary)
                Drawing(state)
            }
            state.floating?.bounds?.containsPixel(pixel) == true -> Moving(state, pixel)
            else -> {
                draw(state.putDown(frame))
                if (state.selection?.containsPixel(pixel) == true) {
                    state.liftSelection(frame)
                    Moving(state, pixel)
                } else {
                    state.selection = null
                    Selecting(state, frame, pixel)
                }
            }
        }
    }
}
