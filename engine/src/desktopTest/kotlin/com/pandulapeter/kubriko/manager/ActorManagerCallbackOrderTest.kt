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

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Disposable
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorManagerCallbackOrderTest {

    private val instance = newManualKubriko()
    private val actorManager = instance.actorManager

    @AfterTest
    fun disposeInstance() = instance.dispose()

    private inner class RecordingActor : Actor, Disposable {
        val events = CopyOnWriteArrayList<String>()

        private fun record(callback: String) {
            val membership = if (this in actorManager.allActors.value) "published" else "unpublished"
            events.add("$callback while $membership on ${Thread.currentThread().name}")
        }

        override fun onAdded(kubriko: Kubriko) = record("onAdded")

        override fun dispose() = record("dispose")

        override fun onRemoved() = record("onRemoved")
    }

    @Test
    fun onAddedRunsRightBeforeTheActorIsPublished() {
        val actor = RecordingActor()

        actorManager.add(actor)
        actorManager.awaitProcessed()

        assertEquals("onAdded while unpublished", actor.events.single().substringBefore(" on "))
    }

    @Test
    fun disposeAndOnRemovedRunInOrderOnOneThreadAfterTheActorIsUnpublished() {
        val actor = RecordingActor()
        actorManager.add(actor)
        actorManager.awaitProcessed()
        actor.events.clear()

        actorManager.remove(actor)
        actorManager.awaitProcessed()

        val thread = actor.events.first().substringAfter(" on ")
        assertEquals(
            listOf(
                "dispose while unpublished on $thread",
                "onRemoved while unpublished on $thread",
            ),
            actor.events.toList(),
        )
    }
}
