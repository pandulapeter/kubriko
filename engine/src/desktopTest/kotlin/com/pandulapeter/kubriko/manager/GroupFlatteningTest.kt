/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupFlatteningTest {

    private class MutableGroup : Actor, Group {
        override var actors: List<Actor> = emptyList()
    }

    @Test
    fun cyclicGroupsAreAddedOnce() {
        val (kubriko, _) = newTestKubriko()
        val g1 = MutableGroup()
        val g2 = MutableGroup()
        val a = CountingActor()
        val b = CountingActor()
        g1.actors = listOf(g2, a)
        g2.actors = listOf(g1, b)
        kubriko.actorManager.add(g1)
        awaitCondition(2_000) { kubriko.actorManager.allActors.value.size == 4 }
        assertEquals(setOf<Actor>(g1, g2, a, b), kubriko.actorManager.allActors.value.toSet())
        val c = CountingActor()
        kubriko.actorManager.add(c)
        kubriko.actorManager.awaitProcessed()
        assertEquals(5, kubriko.actorManager.allActors.value.size)
        kubriko.dispose()
    }

    @Test
    fun sharedChildIsAddedOnce() {
        val (kubriko, _) = newTestKubriko()
        val shared = CountingActor()
        val g1 = MutableGroup().apply { actors = listOf(shared) }
        val g2 = MutableGroup().apply { actors = listOf(shared) }
        kubriko.actorManager.add(g1, g2)
        kubriko.actorManager.awaitProcessed()
        assertEquals(1, kubriko.actorManager.allActors.value.count { it === shared })
        assertEquals(1, shared.added.get())
        kubriko.dispose()
    }
}
