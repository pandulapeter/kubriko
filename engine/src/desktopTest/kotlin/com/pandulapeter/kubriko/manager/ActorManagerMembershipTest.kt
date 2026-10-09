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
import com.pandulapeter.kubriko.actor.traits.Identifiable
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActorManagerMembershipTest {

    private val instance = newManualKubriko()
    private val actorManager = instance.actorManager

    @AfterTest
    fun disposeInstance() = instance.dispose()

    private class UniqueActor : CountingActor(), Unique

    private class OtherUniqueActor : CountingActor(), Unique

    private class NamedActor(override var name: String?) : CountingActor(), Identifiable

    private class EqualActor(val key: Int) : CountingActor() {
        override fun equals(other: Any?) = other is EqualActor && other.key == key

        override fun hashCode() = key
    }

    private fun addAndAwait(vararg actors: Actor) {
        actorManager.add(*actors)
        actorManager.awaitProcessed()
    }

    @Test
    fun addingAnActorAlreadyInTheSceneHasNoEffect() {
        val actor = CountingActor()
        addAndAwait(actor)

        actorManager.add(actor)
        addAndAwait(actor, actor)
        instance.tick()

        assertEquals(1, actorManager.allActors.value.count { it === actor })
        assertEquals(1, actor.added.get())
        assertEquals(1, actor.updates.get())
    }

    @Test
    fun actorAddedSeveralTimesIsRemovedWithOneCall() {
        val actor = CountingActor()
        addAndAwait(actor, actor)

        actorManager.remove(actor)
        actorManager.awaitProcessed()

        assertFalse(actor in actorManager.allActors.value)
        assertEquals(1, actor.removed.get())
    }

    @Test
    fun actorEqualToOneInTheSceneIsTreatedAsAlreadyPresent() {
        val original = EqualActor(key = 1)
        val equal = EqualActor(key = 1)
        addAndAwait(original)

        addAndAwait(equal)

        assertEquals(1, actorManager.allActors.value.size)
        assertEquals(0, equal.added.get())
    }

    @Test
    fun removeAllRemovesEveryActorWithItsCallbacks() {
        val actors = List(3) { CountingActor() }
        addAndAwait(*actors.toTypedArray())

        actorManager.removeAll()
        actorManager.awaitProcessed()

        assertTrue(actorManager.allActors.value.isEmpty())
        actors.forEach {
            assertEquals(1, it.disposed.get())
            assertEquals(1, it.removed.get())
        }
    }

    @Test
    fun addingASecondUniqueOfTheSameTypeReplacesTheFirst() {
        val first = UniqueActor()
        val second = UniqueActor()
        addAndAwait(first)

        addAndAwait(second)

        assertTrue(second in actorManager.allActors.value)
        assertFalse(first in actorManager.allActors.value)
        assertEquals(1, first.removed.get())
        assertEquals(1, second.added.get())
    }

    @Test
    fun reAddingTheUniqueAlreadyInTheSceneHasNoEffect() {
        val unique = UniqueActor()
        addAndAwait(unique)

        addAndAwait(unique)

        assertTrue(unique in actorManager.allActors.value)
        assertEquals(1, unique.added.get())
        assertEquals(0, unique.removed.get())
    }

    @Test
    fun uniquesOfDifferentTypesCoexist() {
        val unique = UniqueActor()
        val otherUnique = OtherUniqueActor()

        addAndAwait(unique, otherUnique)

        assertTrue(unique in actorManager.allActors.value)
        assertTrue(otherUnique in actorManager.allActors.value)
    }

    @Test
    fun unnamedIdentifiableActorsGetDistinctNamesWhenAdded() {
        val first = NamedActor(name = null)
        val second = NamedActor(name = null)

        addAndAwait(first, second)

        assertNotNull(first.name)
        assertNotNull(second.name)
        assertNotEquals(first.name, second.name)
    }

    @Test
    fun namedIdentifiableActorsKeepTheirNamesEvenWhenTheyClash() {
        val first = NamedActor(name = "player")
        val second = NamedActor(name = "player")

        addAndAwait(first, second)

        assertEquals("player", first.name)
        assertEquals("player", second.name)
        assertTrue(first in actorManager.allActors.value)
        assertTrue(second in actorManager.allActors.value)
    }
}
