/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SidebarDragStateTest {
    private var now = 0L
    private val placed = mutableListOf<Pair<String, Offset>>()
    private val moved = mutableListOf<Pair<String, String>>()
    private val ball = SidebarDragState.Dragged("Ball", FolderGroup.ACTOR_TYPES, folder = "")
    private val game = SidebarDragState.Dragged("Game", FolderGroup.MANAGER_TYPES, folder = "rules")
    private val state = SidebarDragState(clock = { now }).apply {
        sceneTarget = SidebarDragState.SceneTarget(Rect(300f, 0f, 700f, 400f)) { name, at -> placed += name to at }
        target(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, ""), Rect(0f, 0f, 260f, 30f))
        target(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "toys"), Rect(0f, 30f, 260f, 60f))
        target(SidebarDragState.FolderKey(FolderGroup.MANAGER_TYPES, ""), Rect(0f, 100f, 260f, 130f))
        onMoveToFolder = { _, name, folder -> moved += name to folder }
    }

    private fun SidebarDragState.target(folder: SidebarDragState.FolderKey, bounds: Rect, id: Any = folder) {
        dropTargets[id] = SidebarDragState.DropTarget(folder, bounds)
    }

    @Test
    fun aTypeDroppedOnAnItemOfAnotherFolderMovesNextToIt() {
        state.target(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, ""), Rect(0f, 200f, 260f, 230f), id = "Paddle")
        state.start(SidebarDragState.Dragged("Bat", FolderGroup.ACTOR_TYPES, folder = "toys"), Offset(10f, 40f))
        state.moveTo(Offset(10f, 210f))
        assertEquals(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, ""), state.hoveredFolder)
        state.end()
        assertEquals(listOf("Bat" to ""), moved)
    }

    @Test
    fun anActorTypeDroppedOnTheSceneIsPlacedThere() {
        state.start(ball, Offset(10f, 200f))
        assertFalse(state.isOverScene)
        state.moveTo(Offset(400f, 50f))
        assertTrue(state.isOverScene)
        state.end()
        assertEquals(listOf("Ball" to Offset(400f, 50f)), placed)
        assertNull(state.dragged)
    }

    @Test
    fun aTypeDroppedOnAFolderOfItsGroupMovesThere() {
        state.start(ball, Offset(10f, 200f))
        state.moveTo(Offset(10f, 40f))
        assertEquals(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "toys"), state.hoveredFolder)
        state.end()
        state.start(game, Offset(10f, 200f))
        state.moveTo(Offset(10f, 40f))
        // A Manager type over a folder of Actor types goes nowhere, and over its own group's header leaves every folder.
        assertNull(state.hoveredFolder)
        state.moveTo(Offset(10f, 110f))
        state.end()
        assertEquals(listOf("Ball" to "toys", "Game" to ""), moved)
    }

    @Test
    fun aSceneDroppedOnAFolderOfAnotherModuleGoesNowhere() {
        state.target(SidebarDragState.FolderKey(FolderGroup.SCENES, "levels", module = "game"), Rect(0f, 300f, 260f, 330f))
        state.target(SidebarDragState.FolderKey(FolderGroup.SCENES, "tests", module = "tools"), Rect(0f, 330f, 260f, 360f))
        state.start(SidebarDragState.Dragged("Intro", FolderGroup.SCENES, folder = "", module = "game"), Offset(10f, 500f))
        state.moveTo(Offset(10f, 340f))
        assertNull(state.hoveredFolder)
        state.moveTo(Offset(10f, 310f))
        state.end()
        assertEquals(listOf("Intro" to "levels"), moved)
    }

    @Test
    fun nothingHappensWhenATypeIsDroppedWhereItIsOrNowhere() {
        state.start(ball, Offset(10f, 200f))
        state.moveTo(Offset(10f, 10f))
        state.end()
        state.start(game, Offset(500f, 50f))
        state.end()
        state.start(ball, Offset(400f, 50f))
        state.cancel()
        assertTrue(placed.isEmpty())
        assertTrue(moved.isEmpty())
    }

    @Test
    fun theClickThatEndsADragIsIgnored() {
        assertFalse(state.isClickAfterDrag)
        state.start(ball, Offset(10f, 10f))
        state.end()
        assertTrue(state.isClickAfterDrag)
        now += 1_000_000_000L
        assertFalse(state.isClickAfterDrag)
    }

    @Test
    fun aDocumentMovesIntoADocumentFolderOrBandOfAnyModuleButNeverIntoAGroup() {
        val state = SidebarDragState()
        val moved = mutableListOf<Triple<FolderGroup?, String, String>>()
        state.onMoveToFolder = { group, name, folder -> moved += Triple(group, name, folder) }
        state.target(SidebarDragState.FolderKey(FolderGroup.PAGES, ""), Rect(0f, 0f, 260f, 30f))
        state.target(SidebarDragState.FolderKey(group = null, path = "editor"), Rect(0f, 30f, 260f, 60f))
        state.start(SidebarDragState.Dragged("game/NOTES.md", group = null, folder = "game", module = "game"), Offset(10f, 10f))
        assertNull(state.hoveredFolder)
        state.moveTo(Offset(10f, 40f))
        assertEquals(SidebarDragState.FolderKey(group = null, path = "editor"), state.hoveredFolder)
        state.end()
        assertEquals(listOf(Triple<FolderGroup?, String, String>(null, "game/NOTES.md", "editor")), moved)
        state.start(ball, Offset(10f, 40f))
        assertNull(state.hoveredFolder)
    }

    @Test
    fun aFolderMovesIntoAnotherButNeverIntoItself() {
        val folderMoves = mutableListOf<Triple<FolderGroup, String, String>>()
        state.onMoveFolder = { group, path, folder -> folderMoves += Triple(group, path, folder) }
        state.target(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "toys/balls"), Rect(0f, 200f, 260f, 230f))
        state.target(SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "toys"), Rect(0f, 230f, 260f, 260f), id = "Ball")
        state.start(SidebarDragState.Dragged("toys", FolderGroup.ACTOR_TYPES, folder = "", isFolder = true), Offset(10f, 40f))
        for (inside in listOf(40f, 210f, 240f)) {
            state.moveTo(Offset(10f, inside))
            assertNull(state.hoveredFolder)
        }
        state.moveTo(Offset(400f, 50f))
        assertFalse(state.isOverScene, "A folder of Actor types isn't placed")
        state.end()
        state.start(SidebarDragState.Dragged("toys/balls", FolderGroup.ACTOR_TYPES, folder = "toys", isFolder = true), Offset(10f, 210f))
        state.moveTo(Offset(10f, 10f))
        state.end()
        assertEquals(listOf(Triple(FolderGroup.ACTOR_TYPES, "toys/balls", "")), folderMoves)
        assertTrue(placed.isEmpty() && moved.isEmpty())
    }
}
