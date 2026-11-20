/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SceneEditorActionsTest {
    private val ball = SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "ball")
    private val handWritten = SceneActorEntry("Spawner", """{"speed":2.0}""", "spawner")
    private val wall = SceneActorEntry("Wall", SceneDocument.blockBuiltState(10f, 20f, 3f, 4f), "wall")

    @Test
    fun copiesGoToTheEndInTheScenesOrderMovedWhenTheySaveAPosition() {
        val copies = duplicatesOf(listOf(ball, handWritten, wall), setOf("wall", "spawner"), offset = 16f)
        assertEquals(listOf(3, 4), copies.map { it.actorIndex })
        assertEquals(listOf("Spawner", "Wall"), copies.map { it.typeId })
        assertEquals(handWritten.state, copies[0].state)
        assertEquals(26f to 36f, SceneDocument.positionOf(copies[1].state))
        val ids = copies.map { it.actorId!! }
        assertEquals(2, ids.toSet().size)
        ids.forEach { assertNotEquals(true, it in setOf("ball", "spawner", "wall")) }
    }
}
