/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UndoRedoHistoryTest {

    private fun snapshot(name: String) = UndoRedoHistory.SceneSnapshot(
        serializedScene = name,
        isSceneModified = false,
        actorIds = LongArray(0),
    )

    @Test
    fun restoredSelectionFollowsTheSelectedId() {
        assertEquals(1, restoredSelectionIndex(selectedId = 2, snapshotIds = longArrayOf(1, 2, 3), restoredCount = 3))
        assertEquals(2, restoredSelectionIndex(selectedId = 3, snapshotIds = longArrayOf(1, 2, 3), restoredCount = 3))
    }

    @Test
    fun nothingIsSelectedWhenTheSelectionCannotBeMatched() {
        assertNull(restoredSelectionIndex(selectedId = null, snapshotIds = longArrayOf(1, 2, 3), restoredCount = 3))
        assertNull(restoredSelectionIndex(selectedId = 4, snapshotIds = longArrayOf(1, 2, 3), restoredCount = 3))
        assertNull(restoredSelectionIndex(selectedId = 2, snapshotIds = longArrayOf(1, 2, 3), restoredCount = 2))
    }

    @Test
    fun undoAndRedoMoveSnapshotsBetweenTheStacks() {
        val history = UndoRedoHistory()
        assertFalse(history.canUndo.value)
        val recorded = snapshot("recorded")
        val current = snapshot("current")
        history.recordAction(recorded)
        assertTrue(history.canUndo.value)
        assertFalse(history.canRedo.value)
        assertSame(recorded, history.performUndo(current))
        assertFalse(history.canUndo.value)
        assertTrue(history.canRedo.value)
        assertSame(current, history.performRedo(recorded))
        assertTrue(history.canUndo.value)
        assertFalse(history.canRedo.value)
    }

    @Test
    fun recordingClearsTheRedoStack() {
        val history = UndoRedoHistory()
        history.recordAction(snapshot("first"))
        history.performUndo(snapshot("current"))
        assertTrue(history.canRedo.value)
        history.recordAction(snapshot("second"))
        assertFalse(history.canRedo.value)
        assertNull(history.performRedo(snapshot("current")))
    }

    @Test
    fun undoStackIsBoundedAndDropsTheOldestSnapshots() {
        val history = UndoRedoHistory()
        repeat(1_000) { history.recordAction(snapshot("$it")) }

        val undone = generateSequence { history.performUndo(snapshot("current"))?.serializedScene }.toList()

        assertTrue(undone.size in 1..<1_000)
        assertEquals((999 downTo 1_000 - undone.size).map { "$it" }, undone)
        assertFalse(history.canUndo.value)
    }
}
