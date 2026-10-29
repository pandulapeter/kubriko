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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorTypeLookTest {
    private val ball = ActorTypeLook(ActorShape.CIRCLE, color = 0xFFFFFFFF, width = 20f, height = 20f)
    private val brick = ActorTypeLook(ActorShape.RECTANGLE, color = 0xFFE0605A, width = 100f, height = 36f)

    @Test
    fun aLookIsTheShapeColorSizeAndSpriteFileOfAType() {
        val sprite = File("/game/src/commonMain/composeResources/drawable/ball.png")
        val looks = ActorTypeLook.of(
            listOf(
                ActorTypeDefinition(name = "Ball", color = 0xFFFFFFFF, width = 20f, height = 20f, sprite = "drawable/ball.png"),
                ActorTypeDefinition(name = "Brick", shape = ActorShape.RECTANGLE, color = 0xFFE0605A, width = 100f, height = 36f, sprite = "drawable/missing.png"),
            ),
            spriteFiles = mapOf("drawable/ball.png" to sprite),
        )
        assertEquals(mapOf("Ball" to ball.copy(spriteFile = sprite), "Brick" to brick), looks)
    }

    @Test
    fun changedAndNewTypesArePreviewedButUnchangedAndRemovedOnesArent() {
        val wall = brick.copy(width = 20f)
        assertEquals(
            mapOf("Brick" to brick.copy(color = 0xFF00FF00), "Wall" to wall),
            ActorTypeLook.previewsOf(
                current = mapOf("Ball" to ball, "Brick" to brick.copy(color = 0xFF00FF00), "Wall" to wall),
                built = mapOf("Ball" to ball, "Brick" to brick, "Paddle" to brick),
            ),
        )
    }
}
