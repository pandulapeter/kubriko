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

import com.pandulapeter.kubrikoCreator.scenes.ScenePatch.Action
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenePatchTest {
    private fun actor(id: String, x: Float = 0f, typeId: String = "Ball", width: Float = 10f) =
        SceneActorEntry(typeId, SceneDocument.blockBuiltState(x, 0f, width, 10f), id)

    private val scene = listOf(actor("a"), actor("b"), actor("c"))

    @Test
    fun anUnchangedSceneKeepsEveryActor() {
        val patch = ScenePatch.between(scene, scene)!!
        assertEquals(listOf(Action.Keep(0), Action.Keep(1), Action.Keep(2)), patch.actions)
        assertEquals(emptyList(), patch.removedIndices)
    }

    @Test
    fun aMovedActorIsOnlyMovedAndAChangedOneIsCreatedAgain() {
        val patch = ScenePatch.between(scene, listOf(actor("a", x = 5f), actor("b", width = 20f), actor("c", typeId = "Wall")))!!
        assertEquals(listOf(Action.Move(0, 5f, 0f), Action.Replace(1), Action.Replace(2)), patch.actions)
    }

    @Test
    fun removedActorsGoAndNewOnesAtTheEndAreAdded() {
        val patch = ScenePatch.between(scene, listOf(actor("a"), actor("c"), actor("d")))!!
        assertEquals(listOf(Action.Keep(0), Action.Keep(2), Action.Add), patch.actions)
        assertEquals(listOf(1), patch.removedIndices)
    }

    @Test
    fun aNewOrderCreatesTheSceneAgain() {
        assertNull(ScenePatch.between(scene, listOf(actor("b"), actor("a"), actor("c"))))
        assertNull(ScenePatch.between(scene, listOf(actor("d"), actor("a"), actor("b"), actor("c"))))
    }

    @Test
    fun onlyAChangeOfPositionCountsAsAMove() {
        val state = SceneDocument.blockBuiltState(1f, 2f, 3f, 4f)
        assertTrue(SceneDocument.isOnlyMoved(state, checkNotNull(SceneDocument.withPosition(state, 5f, 6f))))
        assertTrue(SceneDocument.isOnlyMoved(state, state))
        assertFalse(SceneDocument.isOnlyMoved(state, SceneDocument.blockBuiltState(1f, 2f, 3f, 5f)))
        assertFalse(SceneDocument.isOnlyMoved(state, """{"color":1}"""))
    }
}
