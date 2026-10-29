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
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneActorListTest {
    private val brick = SceneActorEntry("Brick", "{}", "b1")
    private val goldBrick = SceneActorEntry("GoldBrick", "{}", "x7")
    private val ball = SceneActorEntry("Ball", "{}", "brk2")

    @Test
    fun anEmptyFilterShowsEveryActorInOrder() = assertEquals(listOf(brick, goldBrick, ball), matchingSceneActors(listOf(brick, goldBrick, ball), " "))

    @Test
    fun theFilterMatchesTypesAndIdsInAnyCase() {
        assertEquals(listOf(brick, goldBrick), matchingSceneActors(listOf(brick, goldBrick, ball), "brick"))
        assertEquals(listOf(ball), matchingSceneActors(listOf(brick, goldBrick, ball), "BRK"))
        assertEquals(emptyList(), matchingSceneActors(listOf(brick, goldBrick, ball), "paddle"))
    }
}
