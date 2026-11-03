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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DigestTextTest {
    private val game = ManagerTypeDefinition(name = "Game", description = "keeps the score", properties = listOf(PropertyDefinition.Number("score")))
    private val ball = ActorTypeDefinition(
        name = "Ball",
        description = "bounces off the paddle",
        events = listOf(
            EventDefinition(
                type = EventType.COLLISION,
                otherActorType = "Brick",
                blocks = listOf(BlockDefinition.RemoveOther, BlockDefinition.ChangeNumber("score", 10f, manager = "Game"), BlockDefinition.GoToScene("won")),
            ),
        ),
    )
    private val brick = ActorTypeDefinition(name = "Brick", folder = "walls")

    @Test
    fun aSmallGameIsDescribedInFull() {
        val digest = ProjectDigest.of(
            projectName = "breakout",
            types = listOf(brick, game, ball),
            sceneCounts = mapOf("level" to mapOf("Ball" to 1, "Brick" to 40, "SceneSettings" to 1), "won" to emptyMap()),
            sceneDescriptions = mapOf("level" to "the only level"),
        )
        assertEquals(
            """
            Project breakout: 2 Actor types, 1 Manager type, and 2 Scenes.

            Actor types:
            - Ball: bounces off the paddle. Events: When it collides with Brick.
            - walls/Brick.

            Manager types:
            - Game: keeps the score. Properties score: number = 0.

            Scenes:
            - level: the only level. Places Ball, Brick (40).
            - won.

            References:
            - Ball collides with Brick; changes Game.score; goes to Scene won.

            """.trimIndent(),
            DigestText.of(digest),
        )
    }

    @Test
    fun aLargeGameDropsDetailsFirstAndStaysUnderTheBudget() {
        val types = (1..400).map { index ->
            ActorTypeDefinition(
                name = "Enemy$index",
                folder = "enemies/wave${index % 7}",
                description = "an enemy of wave ${index % 7} that flies towards the player",
                properties = listOf(PropertyDefinition.Number("health", 3f)),
                events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Enemy${index % 400 + 1}")),
            )
        }
        val text = DigestText.of(ProjectDigest.of("shooter", types, emptyMap(), emptyMap()))
        assertTrue(text.length <= DigestText.BUDGET, "The digest is ${text.length} characters long.")
        assertFalse("health" in text)
        assertContains(text, "get_relations have the rest")
        val medium = DigestText.of(ProjectDigest.of("shooter", types.take(50), emptyMap(), emptyMap()))
        assertTrue(medium.length <= DigestText.BUDGET)
        assertContains(medium, "flies towards the player")
        assertFalse("health" in medium)
    }
}
