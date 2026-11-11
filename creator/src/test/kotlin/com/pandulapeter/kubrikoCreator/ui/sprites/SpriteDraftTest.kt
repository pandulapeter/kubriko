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
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.commands.resources.EditSpriteFrames
import com.pandulapeter.kubrikoCreator.commands.resources.SaveSprite
import com.pandulapeter.kubrikoCreator.commands.resources.SpriteProblem
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import com.pandulapeter.kubrikoCreator.ui.step
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
        isShown = true
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
    fun editsThatChangeNothingAreNoStepsAndEditsBackToTheSavedPixelsAreNoChange() {
        state.draw(0, listOf(Pixel(0, 0, PixelImage.TRANSPARENT)))
        assertFalse(state.hasChanges, "drawing see-through over see-through")
        assertFalse(state.draft!!.canUndo)
        state.draw(0, listOf(Pixel(0, 0, red)))
        state.draw(0, listOf(Pixel(0, 0, PixelImage.TRANSPARENT)))
        assertFalse(state.hasChanges, "back to what was saved")
        assertTrue(state.draft!!.canUndo, "with its steps still there to undo")
    }

    @Test
    fun theDraftKeepsALimitedNumberOfSteps() {
        repeat(100) { state.draw(0, listOf(Pixel(0, 0, 0xFF000000.toInt() or (it + 1)))) }
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
    fun theDraftIsSavedOnlyOnceItsSaveSucceeded() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        val refusal = Rejection(SpriteProblem.SPRITE_FRAME_MISSING, path, "1", "0")
        assertEquals(refusal, state.save(path, shown(), 0, { _, _, _ -> refusal }) { error("not saved") })
        assertTrue(state.hasChanges)
        var succeed: () -> Unit = {}
        state.save(path, shown(), 0, { _, onSuccess, _ -> null.also { succeed = onSuccess } }) {}
        assertTrue(state.hasChanges, "still changed while the save is under way")
        assertTrue(state.draft!!.canUndo, "and a failing save leaves its steps")
        state.draw(0, listOf(Pixel(1, 0, red)))
        succeed()
        assertTrue(state.hasChanges, "what was drawn since isn't saved")
        assertEquals(red, state.draft!!.saved.frames.single()[0, 0])
        assertEquals(PixelImage.TRANSPARENT, state.draft!!.saved.frames.single()[1, 0])
    }

    @Test
    fun aSaveSucceedingAfterAnotherSpriteIsShownLeavesThatOneAlone() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        var succeed: () -> Unit = {}
        state.save(path, shown(), 0, { _, onSuccess, _ -> null.also { succeed = onSuccess } }) {}
        state.showSprite("drawable/other.png")
        val other = SpriteFrames.of(PixelImage(2, 2), sheet = null)
        state.onRead(other)
        succeed()
        assertSame(other, state.draft!!.saved)
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
    fun keepingOwnChangesOverAnOutsideOneDoesntOfferItAgain() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        val elsewhere = SpriteFrames.of(PixelImage(4, 4).with(listOf(Pixel(3, 3, red))), sheet = null)
        state.onRead(elsewhere)
        state.keepOwnChanges()
        assertNull(state.outsideChange)
        state.onRead(SpriteFrames.of(PixelImage(4, 4).with(listOf(Pixel(3, 3, red))), sheet = null))
        assertNull(state.outsideChange, "the files scanned again bring the same change, already decided about")
        assertTrue(state.hasChanges)
        assertEquals(red, shown()[0, 0])
        state.discard()
        assertSame(elsewhere, state.draft!!.current, "dropping the changes now shows the sprite as it is on the disk")
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
        assertFalse(state.onDraftKey(Key.Y, isShortcut = true, isShift = true) {}, "the workspace's own chords, no others")
    }

    @Test
    fun framesChangeInTheDraftAndTheFrameShownFollowsThem() {
        state.draw(0, listOf(Pixel(0, 0, red)))
        assertNull(state.editFrames(path, EditSpriteFrames.Operation.Duplicate(0)))
        assertEquals(1, state.frameIndex)
        assertEquals(listOf(red, red), state.draft!!.current.frames.map { it[0, 0] })
        assertEquals(2, state.draft!!.current.sheet?.frameCount)
        assertNull(state.editFrames(path, EditSpriteFrames.Operation.Delete(1)))
        assertEquals(0, state.frameIndex)
        assertEquals(SpriteProblem.SPRITE_FRAME_LAST, state.editFrames(path, EditSpriteFrames.Operation.Delete(0))?.reason)
        assertTrue(state.undo())
        assertEquals(2, state.draft!!.current.frames.size, "a frame change is a step of the draft")
    }

    @Test
    fun aDraggedFrameLandsWhereItWasDroppedWithinTheStrip() {
        assertEquals(2, frameDropIndex(from = 0, offset = 130f, pitch = 62f, count = 4))
        assertEquals(0, frameDropIndex(from = 1, offset = -500f, pitch = 62f, count = 4))
        assertEquals(3, frameDropIndex(from = 3, offset = 20f, pitch = 62f, count = 4))
    }

    @Test
    fun theSpeedTypedIsOneStepAndOnlyASpeedTheSheetCanHave() {
        state.editFrames(path, EditSpriteFrames.Operation.Add())
        state.setFramesPerSecond(1f)
        state.setFramesPerSecond(12f)
        state.setFramesPerSecond(0f)
        state.setFramesPerSecond(SpriteSheet.MAXIMUM_FRAMES_PER_SECOND + 1)
        assertEquals(12f, state.draft!!.current.sheet?.framesPerSecond)
        assertTrue(state.undo())
        assertEquals(SpriteSheet.DEFAULT_FRAMES_PER_SECOND, state.draft!!.current.sheet?.framesPerSecond, "typing a speed is undone at once")
        assertEquals(2, state.draft!!.current.frames.size)
        state.setFramesPerSecond(6f)
        state.draw(0, listOf(Pixel(0, 0, red)))
        state.setFramesPerSecond(8f)
        assertTrue(state.undo())
        assertEquals(6f, state.draft!!.current.sheet?.framesPerSecond, "a speed typed after another step is a step of its own")
    }

    @Test
    fun theWorkspaceUndoesOnlyOnceTheShownEditorHasNoStepsOfItsOwn() = kotlinx.coroutines.test.runTest {
        val executor = com.pandulapeter.kubrikoCreator.commands.CommandExecutor(commandContext(), testScope())
        state.draw(0, listOf(Pixel(0, 0, red)))
        state.step(isUndo = true, executor)
        assertFalse(state.hasChanges, "the editor's step was undone")
        state.step(isUndo = false, executor)
        assertTrue(state.hasChanges)
        state.isShown = false
        assertFalse(state.canUndo, "a sprite editor not on screen has no steps to undo")
        state.step(isUndo = true, executor)
        assertTrue(state.hasChanges)
    }
}
