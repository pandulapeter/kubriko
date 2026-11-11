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
    private val lastStepKind: Any? = null,
) {
    constructor(frames: SpriteFrames) : this(frames, frames, emptyList(), emptyList())

    /**
     * Whether [current] holds changes [saved] doesn't. Undoing every step is unchanged again.
     */
    val isChanged: Boolean get() = current !== saved

    val canUndo: Boolean get() = undoSteps.isNotEmpty()

    val canRedo: Boolean get() = redoSteps.isNotEmpty()

    /**
     * This draft with [frames] as one more step, or as part of the last one when both are of [stepKind] (a value typed
     * into a field, changing as it is typed), so undoing goes back past all of it at once; nothing when they hold what
     * [current] does (a stroke over pixels of its own color), and unchanged when they hold what was saved.
     */
    fun with(frames: SpriteFrames, stepKind: Any? = null): SpriteDraft {
        if (frames.isSameAs(current)) return this
        val step = if (frames.isSameAs(saved)) saved else frames
        return if (stepKind != null && stepKind == lastStepKind && undoSteps.isNotEmpty()) {
            SpriteDraft(saved, step, undoSteps, emptyList(), stepKind)
        } else {
            SpriteDraft(saved, step, (undoSteps + current).takeLast(MAXIMUM_STEPS), emptyList(), stepKind)
        }
    }

    fun undone(): SpriteDraft = undoSteps.lastOrNull()?.let { SpriteDraft(saved, it, undoSteps.dropLast(1), redoSteps + current) } ?: this

    fun redone(): SpriteDraft = redoSteps.lastOrNull()?.let { SpriteDraft(saved, it, undoSteps + current, redoSteps.dropLast(1)) } ?: this

    /**
     * This draft once [frames] it held are saved: unchanged when they are still [current], its steps given over to the
     * workspace's history, where the save is one step of its own; otherwise with what was drawn since still a change.
     */
    fun savedAs(frames: SpriteFrames): SpriteDraft = if (frames === current) {
        SpriteDraft(frames, frames, emptyList(), emptyList())
    } else {
        SpriteDraft(frames, current, undoSteps, redoSteps)
    }

    /**
     * This draft with [frames] as what is saved, its changes kept: the sprite as another hand left it, which the user
     * chose to keep their changes over, so reading it again brings nothing new.
     */
    fun rebasedOn(frames: SpriteFrames): SpriteDraft = SpriteDraft(frames, current, undoSteps, redoSteps)

    /**
     * Whether [frames] (as read from the disk) are what was saved, so they bring nothing new: the editor's own save read
     * back, or the files scanned again.
     */
    fun isSaved(frames: SpriteFrames): Boolean = frames.isSameAs(saved)

    /**
     * Whether these frames hold what [other] does: the same sheet and pixels, the frames an edit left alone compared by
     * identity first, so a step compares only what it may have changed.
     */
    private fun SpriteFrames.isSameAs(other: SpriteFrames): Boolean = this === other ||
        (sheet == other.sheet && frames.size == other.frames.size && frames.indices.all { frames[it] === other.frames[it] || frames[it] == other.frames[it] })

    private companion object {
        const val MAXIMUM_STEPS = 64
    }
}
