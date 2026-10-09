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
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.testFixtures.recordingUncaughtExceptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ActorManagerCallbackFailureTest {

    private class CallbackFailure : IllegalStateException()

    @Test
    fun throwingOnAddedDoesNotDropTheRestOfTheBatchAndIsRethrownOnce(): Unit = recordingUncaughtExceptions { recorded ->
        newManualKubriko().use { instance ->
            val before = CountingActor()
            val thrower = CountingActor(onAddedAction = { throw CallbackFailure() })
            val after = CountingActor()

            instance.actorManager.add(before, thrower, after)
            instance.actorManager.awaitProcessed()

            assertEquals(listOf<Actor>(before, thrower, after), instance.actorManager.allActors.value.toList())
            assertEquals(1, after.added.get())
            awaitCondition { recorded.isNotEmpty() }
            assertIs<CallbackFailure>(recorded.single())
        }
    }

    @Test
    fun throwingDisposeStillRemovesEveryActorWithItsOnRemoved(): Unit = recordingUncaughtExceptions { recorded ->
        newManualKubriko().use { instance ->
            val first = CountingActor()
            val thrower = CountingActor(disposeAction = { throw CallbackFailure() })
            val last = CountingActor()
            instance.actorManager.add(first, thrower, last)
            instance.actorManager.awaitProcessed()

            instance.actorManager.remove(first, thrower, last)
            instance.actorManager.awaitProcessed()

            assertTrue(instance.actorManager.allActors.value.isEmpty())
            listOf(first, thrower, last).forEach { assertEquals(1, it.removed.get()) }
            awaitCondition { recorded.isNotEmpty() }
            assertIs<CallbackFailure>(recorded.single())
        }
    }

    @Test
    fun throwingOnRemovedDoesNotStopTheOtherRemovals(): Unit = recordingUncaughtExceptions { recorded ->
        newManualKubriko().use { instance ->
            val thrower = CountingActor(onRemovedAction = { throw CallbackFailure() })
            val other = CountingActor()
            instance.actorManager.add(thrower, other)
            instance.actorManager.awaitProcessed()

            instance.actorManager.remove(thrower, other)
            instance.actorManager.awaitProcessed()

            assertTrue(instance.actorManager.allActors.value.isEmpty())
            assertEquals(1, other.removed.get())
            awaitCondition { recorded.isNotEmpty() }
        }
    }

    @Test
    fun laterOperationsAreStillProcessedAfterAFailure(): Unit = recordingUncaughtExceptions { recorded ->
        newManualKubriko().use { instance ->
            instance.actorManager.add(CountingActor(onAddedAction = { throw CallbackFailure() }))
            awaitCondition { recorded.isNotEmpty() }
            val actor = CountingActor()

            instance.actorManager.add(actor)
            instance.actorManager.awaitProcessed()

            assertTrue(actor in instance.actorManager.allActors.value)
        }
    }
}
