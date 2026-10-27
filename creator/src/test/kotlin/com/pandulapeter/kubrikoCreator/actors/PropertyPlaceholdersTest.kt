/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyPlaceholdersTest {

    @Test
    fun placeholdersNameAPropertyAndOptionallyItsManagerType() {
        assertEquals(
            listOf(PropertyPlaceholders.Placeholder(0..6, null, "score"), PropertyPlaceholders.Placeholder(8..19, "Game", "lives")),
            PropertyPlaceholders.find("{score} {Game.lives} {Score} {game.x} {}").toList(),
        )
    }

    @Test
    fun renamingAPropertyChangesOnlyItsOwnPlaceholders() {
        assertEquals("{points} {Game.score}", PropertyPlaceholders.withPropertyRenamed("{score} {Game.score}", null, "score", "points"))
        assertEquals("{score} {Game.points} {Level.score}", PropertyPlaceholders.withPropertyRenamed("{score} {Game.score} {Level.score}", "Game", "score", "points"))
    }

    @Test
    fun renamingAManagerTypeChangesItsPlaceholders() {
        assertEquals("Score: {Match.score}, {score}, {Matchless.x}", PropertyPlaceholders.withManagerRenamed("Score: {Game.score}, {score}, {Matchless.x}", "Game", "Match"))
    }
}
