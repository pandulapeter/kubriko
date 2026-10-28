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
        folderTargets[SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "")] = Rect(0f, 0f, 260f, 30f)
        folderTargets[SidebarDragState.FolderKey(FolderGroup.ACTOR_TYPES, "toys")] = Rect(0f, 30f, 260f, 60f)
        folderTargets[SidebarDragState.FolderKey(FolderGroup.MANAGER_TYPES, "")] = Rect(0f, 100f, 260f, 130f)
        onMoveToFolder = { _, name, folder -> moved += name to folder }
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
}
