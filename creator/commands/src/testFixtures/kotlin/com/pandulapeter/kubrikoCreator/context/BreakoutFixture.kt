/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument

/**
 * A Breakout game the size the scratch copy has grown to, for measuring what the AI's tools cost on a real game.
 */
public object BreakoutFixture {

    val ball = ActorTypeDefinition(
        name = "Ball",
        description = "the ball the player keeps in play with the paddle",
        width = 20f,
        height = 20f,
        color = 0xFFFFFFFF,
        properties = listOf(PropertyDefinition.Number("speed", 300f)),
        events = listOf(
            EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.SetVelocity(200f, -300f))),
            EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge, BlockDefinition.PlaySound("files/sounds/wall.wav"))),
            EventDefinition(
                type = EventType.COLLISION,
                otherActorType = "Brick",
                blocks = listOf(
                    BlockDefinition.BounceOffOther,
                    BlockDefinition.RemoveOther,
                    BlockDefinition.ChangeNumber("score", 10f, manager = "Game"),
                    BlockDefinition.PlaySound("files/sounds/hit.wav"),
                ),
            ),
            EventDefinition(EventType.COLLISION, otherActorType = "Paddle", blocks = listOf(BlockDefinition.BounceOffOther, BlockDefinition.AddToVelocity(0f, -20f))),
            EventDefinition(
                type = EventType.EVERY_FRAME,
                blocks = listOf(
                    BlockDefinition.If(ConditionDefinition.Velocity(comparison = Comparison.GREATER, value = 600f)),
                    BlockDefinition.SetVelocity(400f, 400f),
                    BlockDefinition.End,
                ),
            ),
        ),
    )
    val paddle = ActorTypeDefinition(
        name = "Paddle",
        description = "what the player moves along the bottom",
        shape = ActorShape.RECTANGLE,
        width = 120f,
        height = 20f,
        events = listOf(
            EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.LEFT, blocks = listOf(BlockDefinition.SetVelocity(-400f, 0f))),
            EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.of("DirectionRight"), blocks = listOf(BlockDefinition.SetVelocity(400f, 0f))),
            EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.LEFT, isHeld = false)), BlockDefinition.Stop, BlockDefinition.End)),
            EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.Stop)),
        ),
    )
    val brick = ActorTypeDefinition(name = "Brick", description = "a brick the ball breaks", shape = ActorShape.RECTANGLE, width = 60f, height = 24f)
    val goldBrick = ActorTypeDefinition(name = "GoldBrick", parent = "Brick", description = "a brick worth more", color = 0xFFFFC857)
    val game = ManagerTypeDefinition(
        name = "Game",
        description = "the score and the lives",
        properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Number("lives", 3f)),
        events = listOf(
            EventDefinition(
                type = EventType.EVERY_FRAME,
                blocks = listOf(BlockDefinition.If(ConditionDefinition.Number("lives", Comparison.LESS_OR_EQUAL, 0f)), BlockDefinition.GoToScene("game-over"), BlockDefinition.End),
            ),
        ),
        texts = listOf(ScreenTextDefinition("Score: {score}"), ScreenTextDefinition("Lives: {lives}")),
    )
    val types = listOf(ball, paddle, brick, goldBrick, game)

    /**
     * A level of 40 Bricks, four of them gold, with the Paddle and the Ball.
     */
    val level = (0 until 40).map { index ->
        SceneActorEntry(
            typeId = if (index % 10 == 4) "GoldBrick" else "Brick",
            state = SceneDocument.blockBuiltState(x = -270f + index % 10 * 60f, y = -300f + index / 10 * 30f, width = 60f, height = 24f),
            id = "b$index",
        )
    } + listOf(
        SceneActorEntry("Paddle", SceneDocument.blockBuiltState(0f, 300f, 120f, 20f), "paddle"),
        SceneActorEntry("Ball", SceneDocument.blockBuiltState(0f, 250f, 20f, 20f), "ball"),
    )
}
