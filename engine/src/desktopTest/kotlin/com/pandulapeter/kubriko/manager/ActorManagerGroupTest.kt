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
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActorManagerGroupTest {

    private val instance = newManualKubriko()
    private val actorManager = instance.actorManager

    @AfterTest
    fun disposeInstance() = instance.dispose()

    private class TestGroup(vararg actors: Actor) : Actor, Group {
        override var actors: List<Actor> = actors.toList()
    }

    @Test
    fun addingAGroupAddsItAndItsNestedChildren() {
        val child = CountingActor()
        val nestedChild = CountingActor()
        val group = TestGroup(child, TestGroup(nestedChild))

        actorManager.add(group)
        actorManager.awaitProcessed()

        assertEquals(4, actorManager.allActors.value.size)
        assertTrue(group in actorManager.allActors.value)
        assertEquals(1, child.added.get())
        assertEquals(1, nestedChild.added.get())
    }

    @Test
    fun removingAGroupRemovesItsChildren() {
        val children = List(3) { CountingActor() }
        val group = TestGroup(*children.toTypedArray())
        actorManager.add(group)
        actorManager.awaitProcessed()

        actorManager.remove(group)
        actorManager.awaitProcessed()

        assertTrue(actorManager.allActors.value.isEmpty())
        children.forEach { assertEquals(1, it.removed.get()) }
    }

    @Test
    fun cyclicGroupsAreAddedOnce() {
        val first = TestGroup()
        val second = TestGroup()
        val firstChild = CountingActor()
        val secondChild = CountingActor()
        first.actors = listOf(second, firstChild)
        second.actors = listOf(first, secondChild)

        actorManager.add(first)
        actorManager.awaitProcessed()

        assertEquals(setOf<Actor>(first, second, firstChild, secondChild), actorManager.allActors.value.toSet())
        assertEquals(4, actorManager.allActors.value.size)
    }

    @Test
    fun childSharedBySeveralGroupsIsAddedOnce() {
        val shared = CountingActor()

        actorManager.add(TestGroup(shared), TestGroup(shared))
        actorManager.awaitProcessed()

        assertEquals(1, actorManager.allActors.value.count { it === shared })
        assertEquals(1, shared.added.get())
    }

    @Test
    fun removingAnyGroupRemovesASharedChild() {
        val shared = CountingActor()
        val first = TestGroup(shared)
        val second = TestGroup(shared)
        actorManager.add(first, second)
        actorManager.awaitProcessed()

        actorManager.remove(first)
        actorManager.awaitProcessed()

        assertEquals(listOf<Actor>(second), actorManager.allActors.value.toList())
        assertEquals(1, shared.removed.get())
    }
}
