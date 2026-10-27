/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneDocumentTest {
    private val ballState = """{"body":{"position":{"x":-200.0,"y":-100.0},"size":{"width":80.0,"height":80.0}},"color":-44929713012998144}"""
    private val scene = """[{"typeId":"Ball","state":"{\"body\":{\"position\":{\"x\":-200.0,\"y\":-100.0},\"size\":{\"width\":80.0,\"height\":80.0}},\"color\":-44929713012998144}"}]"""

    @Test
    fun scenesAreReadAndWrittenAsKubrikoWritesThem() {
        val actors = SceneDocument.parse(scene)
        assertEquals(listOf(SceneActorEntry("Ball", ballState)), actors)
        assertEquals(scene, SceneDocument.format(actors))
    }

    @Test
    fun onlyListsOfActorsWithObjectStatesAreValid() {
        assertTrue(SceneDocument.isValid("[]"))
        assertTrue(SceneDocument.isValid(scene))
        assertFalse(SceneDocument.isValid("{}"))
        assertFalse(SceneDocument.isValid("""[{"typeId":"Ball"}]"""))
        assertFalse(SceneDocument.isValid("""[{"typeId":"Ball","state":"[1]"}]"""))
        assertFalse(SceneDocument.isValid("not json"))
    }

    @Test
    fun positionsAreReadAndReplacedKeepingTheRestOfTheState() {
        assertEquals(-200f to -100f, SceneDocument.positionOf(ballState))
        val moved = SceneDocument.withPosition(ballState, 5f, 6.5f)!!
        assertEquals(5f to 6.5f, SceneDocument.positionOf(moved))
        assertEquals(ballState, SceneDocument.withPosition(moved, -200f, -100f))
        assertNull(SceneDocument.positionOf("""{"color":1}"""))
        assertNull(SceneDocument.withPosition("""{"color":1}""", 0f, 0f))
    }

    @Test
    fun blockBuiltActorsArePlacedWithOnlyTheirBody() {
        val state = SceneDocument.blockBuiltState(1f, 2f, 30f, 40f)
        assertEquals("""{"body":{"position":{"x":1.0,"y":2.0},"size":{"width":30.0,"height":40.0}}}""", state)
        assertEquals(1f to 2f, SceneDocument.positionOf(state))
    }
}
