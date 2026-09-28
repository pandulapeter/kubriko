/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.recordingUncaughtExceptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActorCallbackFailureTest {

    @Test
    fun throwingOnAddedDoesNotDropTheRestOfTheBatch() = recordingUncaughtExceptions { recorded ->
        val (kubriko, _) = newTestKubriko()
        val before = CountingActor()
        val thrower = CountingActor(onAddedAction = { throw IllegalStateException("onAdded failed") })
        val after = CountingActor()
        kubriko.actorManager.add(before, thrower, after)
        kubriko.actorManager.awaitProcessed()
        assertEquals(listOf(before, thrower, after), kubriko.actorManager.allActors.value.toList())
        assertEquals(1, before.added.get())
        assertEquals(1, after.added.get())
        awaitCondition { recorded.isNotEmpty() }
        assertEquals(1, recorded.size)
        assertTrue(recorded.single() is IllegalStateException)
        kubriko.dispose()
    }

    @Test
    fun throwingDisposeStillCallsOnRemovedAndOtherRemovals() = recordingUncaughtExceptions { recorded ->
        val (kubriko, _) = newTestKubriko()
        val first = CountingActor()
        val middle = CountingActor(disposeAction = { throw IllegalStateException("dispose failed") })
        val last = CountingActor()
        kubriko.actorManager.add(first, middle, last)
        kubriko.actorManager.awaitProcessed()
        kubriko.actorManager.remove(first, middle, last)
        kubriko.actorManager.awaitProcessed()
        assertTrue(kubriko.actorManager.allActors.value.isEmpty())
        listOf(first, middle, last).forEach { assertEquals(1, it.removed.get()) }
        awaitCondition { recorded.isNotEmpty() }
        kubriko.dispose()
    }

    @Test
    fun processorKeepsWorkingAfterAFailure() = recordingUncaughtExceptions { recorded ->
        val (kubriko, _) = newTestKubriko()
        kubriko.actorManager.add(CountingActor(onAddedAction = { throw IllegalStateException("onAdded failed") }))
        awaitCondition { recorded.isNotEmpty() }
        val x = CountingActor()
        kubriko.actorManager.add(x)
        awaitCondition { x in kubriko.actorManager.allActors.value }
        kubriko.dispose()
    }
}
