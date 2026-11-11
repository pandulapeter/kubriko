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

import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames

/**
 * What the sprite editor holds of a sprite: its frames as [saved], and as they are now ([current]), with the steps to
 * undo and redo between them. The sprite editor keeps its changes until they are saved, as an image editor does, rather
 * than saving every stroke as the rest of Kubriko Creator saves every change, so a half-drawn sprite reaches neither the
 * game nor git, and its own undo steps go back stroke by stroke until then.
 */
internal class SpriteDraft private constructor(
    val saved: SpriteFrames,
    val current: SpriteFrames,
    private val undoSteps: List<SpriteFrames>,
    private val redoSteps: List<SpriteFrames>,
) {
    constructor(frames: SpriteFrames) : this(frames, frames, emptyList(), emptyList())

    /**
     * Whether [current] holds changes [saved] doesn't. Undoing every step is unchanged again.
     */
    val isChanged: Boolean get() = current !== saved

    val canUndo: Boolean get() = undoSteps.isNotEmpty()

    val canRedo: Boolean get() = redoSteps.isNotEmpty()

    /**
     * This draft with [frames] as one more step.
     */
    fun with(frames: SpriteFrames): SpriteDraft = SpriteDraft(saved, frames, (undoSteps + current).takeLast(MAXIMUM_STEPS), emptyList())

    fun undone(): SpriteDraft = undoSteps.lastOrNull()?.let { SpriteDraft(saved, it, undoSteps.dropLast(1), redoSteps + current) } ?: this

    fun redone(): SpriteDraft = redoSteps.lastOrNull()?.let { SpriteDraft(saved, it, undoSteps + current, redoSteps.dropLast(1)) } ?: this

    /**
     * This draft as it is once [current] is saved: unchanged, its steps given over to the workspace's history, where the
     * save is one step of its own.
     */
    fun markedSaved(): SpriteDraft = SpriteDraft(current, current, emptyList(), emptyList())

    /**
     * This draft with what was saved before as [saved] again, its save having failed, so what it saved counts as a change
     * again (and whatever was drawn since).
     */
    fun unsaved(previouslySaved: SpriteFrames): SpriteDraft = SpriteDraft(previouslySaved, current, undoSteps, redoSteps)

    /**
     * Whether [frames] (as read from the disk) are what was saved, so they bring nothing new: the editor's own save read
     * back, or the files scanned again.
     */
    fun isSaved(frames: SpriteFrames): Boolean = frames.sheet == saved.sheet && frames.toImage() == saved.toImage()

    private companion object {
        const val MAXIMUM_STEPS = 64
    }
}
