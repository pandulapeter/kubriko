/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import kotlin.test.Test
import kotlin.test.assertEquals

class MentionTest {

    @Test
    fun `every kind of file is told by its path`() {
        val scene = "src/commonMain/composeResources/files/scenes/levels/1-1.json"
        assertEquals(
            listOf(
                Mention.ActorType("Bat", ".kubriko/actors/enemies/Bat.json"),
                Mention.ManagerType("Score", ".kubriko/managers/Score.json"),
                Mention.Scene("levels/1-1", scene),
                Mention.Page("levels/cave", "screenplay/levels/cave.md"),
                Mention.Document("docs/combat.md"),
                Mention.Sprite("game/src/commonMain/composeResources/drawable/ball.png"),
                Mention.Audio("src/commonMain/composeResources/files/sounds/hit.wav"),
                Mention.Kotlin("src/commonMain/kotlin/Game.kt"),
                Mention.OtherFile("gradle.properties"),
                Mention.OtherFile("src/mycomposeResources/drawable/ball.png"),
                Mention.OtherFile("src/commonMain/composeResources/drawable/nested/ball.png"),
                Mention.OtherFile(".kubriko/project.json"),
            ),
            listOf(
                ".kubriko/actors/enemies/Bat.json",
                ".kubriko/managers/Score.json",
                scene,
                "screenplay/levels/cave.md",
                "docs/combat.md",
                "game/src/commonMain/composeResources/drawable/ball.png",
                "src/commonMain/composeResources/files/sounds/hit.wav",
                "src/commonMain/kotlin/Game.kt",
                "gradle.properties",
                "src/mycomposeResources/drawable/ball.png",
                "src/commonMain/composeResources/drawable/nested/ball.png",
                ".kubriko/project.json",
            ).map(Mention::of),
        )
    }
}
