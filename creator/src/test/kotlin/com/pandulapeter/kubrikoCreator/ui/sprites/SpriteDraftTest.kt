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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SaveSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SpriteProblem
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpriteDraftTest {
    private val red = 0xFFFF0000.toInt()
    private val path = "drawable/hero.png"
    private val original = SpriteFrames.of(PixelImage(4, 4), sheet = null)
    private val state = SpriteEditorState().apply {
        showSprite(path)
        onRead(original)
    }

    private fun shown() = state.draft!!.current.frames.single()

    @Test
    fun strokesAreUndoneAndRedoneOneByOneAndUndoingThemAllIsUnchanged() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        state.draw(0, listOf(Pixel(1, 0, red)))
        assertTrue(state.hasChanges)
        assertTrue(state.undo())
        assertEquals(PixelImage(4, 4).with(listOf(Pixel(0, 0, red))), shown())
        assertTrue(state.undo())
        assertFalse(state.hasChanges, "back where it was saved")
        assertFalse(state.undo(), "nothing left to undo goes on to the workspace")
        assertTrue(state.redo())
        assertTrue(state.redo())
        assertFalse(state.redo())
        assertEquals(PixelImage(4, 4).with(listOf(Pixel(0, 0, red), Pixel(1, 0, red))), shown())
        state.undo()
        state.draw(0, listOf(Pixel(3, 3, red)))
        assertFalse(state.draft!!.canRedo, "a new step drops what was undone")
    }

    @Test
    fun theDraftKeepsALimitedNumberOfSteps() {
        repeat(100) { state.draw(0, listOf(Pixel(it % 4, it / 4 % 4, if (it % 2 == 0) red else PixelImage.TRANSPARENT))) }
        var undone = 0
        while (state.undo()) undone++
        assertEquals(64, undone)
        assertTrue(state.hasChanges, "the steps before the oldest kept can't be undone")
    }

    @Test
    fun imageOperationsChangeTheDraftAsTheCommandWouldAndSayWhyTheyCant() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        assertNull(state.edit(path, EditSprite.Operation.Flip(isVertical = false), frameIndex = null))
        assertEquals(red, shown()[3, 0])
        assertNotNull(state.edit(path, EditSprite.Operation.Scale(EditSprite.MAXIMUM_SCALE + 1), frameIndex = null))
        assertEquals(red, shown()[3, 0], "a refused operation changes nothing")
    }

    @Test
    fun savingSendsTheFramesAsOneCommandAndMarksThemSaved() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        val sent = mutableListOf<Invocation<*>>()
        var saved = 0
        val rejection = state.save(path, shown(), 0, { invocation, onSuccess, _ -> null.also { sent += invocation; onSuccess() } }) { saved++ }
        assertNull(rejection)
        assertEquals(1, saved)
        val input = sent.single().input as SaveSprite.Input
        assertEquals(path, input.sprite)
        assertEquals(PixelImage(4, 4).with(listOf(Pixel(0, 0, red))), input.frames.frames.single())
        assertFalse(state.hasChanges)
        assertFalse(state.draft!!.canUndo, "the save is the workspace's step now")
        state.save(path, shown(), 0, { _, _, _ -> error("nothing to save") }) { saved++ }
        assertEquals(2, saved, "with nothing to save, it goes on at once")
    }

    @Test
    fun aSaveRefusedOrFailingBringsTheChangesBack() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        val refusal = Rejection(SpriteProblem.SPRITE_FRAME_MISSING, path, "1", "0")
        assertEquals(refusal, state.save(path, shown(), 0, { _, _, _ -> refusal }) { error("not saved") })
        assertTrue(state.hasChanges)
        var fail: () -> Unit = {}
        state.save(path, shown(), 0, { _, _, onFailure -> null.also { fail = onFailure } }) { error("not saved") }
        assertFalse(state.hasChanges)
        fail()
        assertTrue(state.hasChanges)
        assertSame(original, state.draft!!.saved)
    }

    @Test
    fun theSavedFramesReadBackBringNothingNewWhileAChangeElsewhereReplacesOnlyAnUnchangedDraft() {
        state.onRead(SpriteFrames.of(PixelImage(4, 4), sheet = null))
        assertSame(original, state.draft!!.saved, "the same pixels read again")
        val elsewhere = SpriteFrames.of(PixelImage(4, 4).with(listOf(Pixel(2, 2, red))), sheet = null)
        state.onRead(elsewhere)
        assertSame(elsewhere, state.draft!!.current, "an unchanged draft follows the file")
        state.draw(0, listOf(Pixel(0, 0, red)))
        val again = SpriteFrames.of(PixelImage(4, 4).with(listOf(Pixel(3, 3, red))), sheet = null)
        state.onRead(again)
        assertSame(again, state.outsideChange, "a changed draft is kept, and the change offered")
        assertEquals(red, shown()[0, 0])
        state.loadOutsideChange()
        assertSame(again, state.draft!!.current)
        assertFalse(state.hasChanges)
        assertNull(state.outsideChange)
    }

    @Test
    fun discardingGoesBackToWhatWasSavedAndDropsWhatFloats() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        state.floating = FloatingPixels(PixelImage(1, 1), androidx.compose.ui.unit.IntOffset.Zero)
        state.discard()
        assertFalse(state.hasChanges)
        assertNull(state.floating)
        assertEquals(PixelImage(4, 4), shown())
    }

    @Test
    fun shortcutsSaveAndStepThroughTheDraftAndLeaveTheRestToTheWorkspace() {
        var saves = 0
        assertTrue(state.onDraftKey(Key.S, isShortcut = true, isShift = false) { saves++ })
        assertEquals(1, saves)
        assertFalse(state.onDraftKey(Key.Z, isShortcut = true, isShift = false) {}, "no steps: the workspace undoes")
        state.draw(0, listOf(Pixel(0, 0, red)))
        assertTrue(state.onDraftKey(Key.Z, isShortcut = true, isShift = false) {})
        assertTrue(state.onDraftKey(Key.Z, isShortcut = true, isShift = true) {})
        assertTrue(state.onDraftKey(Key.Z, isShortcut = true, isShift = false) {})
        assertTrue(state.onDraftKey(Key.Y, isShortcut = true, isShift = false) {})
        assertEquals(red, shown()[0, 0])
        assertFalse(state.onDraftKey(Key.S, isShortcut = false, isShift = false) {}, "S alone is the selection tool's")
    }
}
