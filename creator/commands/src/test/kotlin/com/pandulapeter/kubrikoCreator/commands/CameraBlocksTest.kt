/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The camera Blocks: what validating them accepts and refuses, and how the AI reads them.
 */
class CameraBlocksTest {
    private val hero = ActorTypeDefinition(
        name = "Hero",
        properties = listOf(PropertyDefinition.Number("zoom")),
        events = listOf(EventDefinition(EventType.ADDED_TO_SCENE)),
    )
    private val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.GAME_STARTED)))

    private fun CommandContext.rejection(block: BlockDefinition, type: String = "Hero") = AddBlock.validate(this, AddBlock.Input(type, 0, block))

    @Test
    fun theCameraMovesShakesAndZoomsWithNumbersThatAreRandomOrAPropertysValue() = runTest {
        val context = commandContext(hero, game)
        listOf(
            BlockDefinition.FollowWithCamera,
            BlockDefinition.MoveCamera(NumberValue.Edge(SceneEdge.LEFT, 100f), NumberValue.Property("zoom")),
            BlockDefinition.ShakeCamera(NumberValue.Random(0f, 20f), NumberValue.Fixed(0f)),
            BlockDefinition.ZoomCamera(NumberValue.Property("zoom")),
            BlockDefinition.ZoomCamera(NumberValue.Random(1f, 300f)),
        ).forEach { assertNull(context.rejection(it), it.toString()) }
        assertNull(context.rejection(BlockDefinition.ZoomCamera(), type = "Game"))
    }

    @Test
    fun onlyAnActorCanBeFollowed() = runTest {
        assertEquals(Rejection(PlacementProblem.BLOCK_FOLLOW_WITH_CAMERA_IN_MANAGER_TYPE), commandContext(hero, game).rejection(BlockDefinition.FollowWithCamera, type = "Game"))
    }

    @Test
    fun aZoomIsAtLeastOnePercentAndAShakeNeverNegative() = runTest {
        val context = commandContext(hero)
        assertEquals(Rejection(CommandProblem.ZOOM_OUT_OF_RANGE), context.rejection(BlockDefinition.ZoomCamera(NumberValue.Fixed(0f))))
        assertEquals(Rejection(CommandProblem.ZOOM_OUT_OF_RANGE), context.rejection(BlockDefinition.ZoomCamera(NumberValue.Random(0.5f, 2f))))
        assertEquals(Rejection(CommandProblem.SHAKE_NEGATIVE), context.rejection(BlockDefinition.ShakeCamera(strength = NumberValue.Fixed(-1f))))
        assertEquals(Rejection(CommandProblem.SHAKE_NEGATIVE), context.rejection(BlockDefinition.ShakeCamera(durationInMilliseconds = NumberValue.Random(-5f, 5f))))
    }

    @Test
    fun onlyWhereTheCameraMovesCanBeAnEdge() = runTest {
        val context = commandContext(hero)
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_ACROSS), context.rejection(BlockDefinition.MoveCamera(x = NumberValue.Edge(SceneEdge.TOP))))
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.ShakeCamera(strength = NumberValue.Edge(SceneEdge.LEFT))))
        assertEquals(Rejection(CommandProblem.NUMBER_EDGE_NOT_TAKEN), context.rejection(BlockDefinition.ZoomCamera(NumberValue.Edge(SceneEdge.RIGHT))))
        assertContains(CommandProblem.NUMBER_EDGE_NOT_TAKEN.english, "a shake's strength, a zoom")
    }

    @Test
    fun theAiReadsWhatEachCameraBlockDoes() {
        assertEquals("Follow this Actor with the camera", BlockNotation.of(BlockDefinition.FollowWithCamera))
        assertEquals("Move the camera to (0, 0)", BlockNotation.of(BlockDefinition.MoveCamera()))
        assertEquals("Shake the camera 8 far for 300 ms", BlockNotation.of(BlockDefinition.ShakeCamera()))
        assertEquals("Zoom the camera to 150 percent", BlockNotation.of(BlockDefinition.ZoomCamera(NumberValue.Fixed(150f))))
    }
}
