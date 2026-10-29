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

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoCreator.actors.ActorShape
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewActorTest {
    private val look = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFFE0605A, width = 100f, height = 36f)

    @Test
    fun theBodyAndColorComeFromTheActorsState() {
        val state = ActorStateEdits.withChanges(
            SceneDocument.blockBuiltState(10f, 20f, 40f, 30f),
            ActorStateEdits.Changes(rotationInDegrees = 90f, scaleHorizontal = 2f, scaleVertical = 3f, color = 0xFF00FF00),
        )!!
        val actor = PreviewActor(state, look)
        assertEquals(SceneOffset(10f.sceneUnit, 20f.sceneUnit), actor.body.position)
        assertEquals(SceneSize(40f.sceneUnit, 30f.sceneUnit), actor.body.size)
        assertEquals(SceneOffset(20f.sceneUnit, 15f.sceneUnit), actor.body.pivot)
        assertEquals(Scale(2f, 3f), actor.body.scale)
        assertEquals((Math.PI / 2).toFloat(), actor.body.rotation.raw, 1e-6f)
        assertEquals(Color(0xFF00FF00), actor.color)
    }

    @Test
    fun whatTheStateLeavesOutComesFromTheLook() {
        val actor = PreviewActor("{}", look)
        assertEquals(SceneOffset.Zero, actor.body.position)
        assertEquals(SceneSize(100f.sceneUnit, 36f.sceneUnit), actor.body.size)
        assertEquals(SceneOffset(50f.sceneUnit, 18f.sceneUnit), actor.body.pivot)
        assertEquals(Color(0xFFE0605A), actor.color)
    }

    @Test
    fun savingKeepsTheStateWithThePositionTheActorWasMovedTo() {
        val actor = PreviewActor(SceneDocument.blockBuiltState(10f, 20f, 40f, 30f), look)
        actor.body.position = SceneOffset(5f.sceneUnit, 6f.sceneUnit)
        assertEquals(SceneDocument.positionOf(SceneDocument.blockBuiltState(5f, 6f, 40f, 30f)), SceneDocument.positionOf(actor.save().serialize()))
    }
}
