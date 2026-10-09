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

import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.CountingActor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class ActorBatchProcessorTest {

    private val loggedMessages = mutableListOf<String>()

    private fun newProcessor() = ActorBatchProcessor(shouldComposeLayers = false) { message, _ ->
        loggedMessages.add(message)
    }

    @Test
    fun operationsIssuedBeforeStartApplySynchronouslyOnTheStartingThreadInIssueOrder() {
        val (kubriko, _) = newTestKubriko()
        val processor = newProcessor()
        val first = CountingActor()
        val second = CountingActor()
        val third = CountingActor()
        processor.add(listOf(first, second))
        processor.remove(listOf(first))
        processor.add(listOf(third))
        processor.start(kubriko, kubriko, kubriko.dispatcher)
        assertEquals(listOf(second, third), processor.allActors.value.toList())
        assertEquals(listOf(second, third), processor.dynamicActors.value.toList())
        assertSame(Thread.currentThread(), second.onAddedThread.get())
        assertSame(Thread.currentThread(), third.onAddedThread.get())
        assertEquals(1, first.removed.get())
        processor.dispose()
        kubriko.dispose()
    }

    @Test
    fun addThenRemoveInOneBatchPairsTheCallbacksAndNeverPublishes() {
        val (kubriko, _) = newTestKubriko()
        val processor = newProcessor()
        var wasPublishedAtRemoval = true
        lateinit var actor: CountingActor
        actor = CountingActor(onRemovedAction = { wasPublishedAtRemoval = actor in processor.allActors.value })
        processor.add(listOf(actor))
        processor.remove(listOf(actor))
        processor.start(kubriko, kubriko, kubriko.dispatcher)
        assertEquals(1, actor.added.get())
        assertEquals(1, actor.disposed.get())
        assertEquals(1, actor.removed.get())
        assertFalse(wasPublishedAtRemoval)
        assertEquals(emptyList(), loggedMessages)
        processor.dispose()
        kubriko.dispose()
    }

    @Test
    fun disposeDisposesTheRemainingActorsWithoutRemovingThem() {
        val (kubriko, _) = newTestKubriko()
        val processor = newProcessor()
        val actor = CountingActor()
        processor.add(listOf(actor))
        processor.start(kubriko, kubriko, kubriko.dispatcher)
        processor.dispose()
        assertEquals(1, actor.disposed.get())
        assertEquals(0, actor.removed.get())
        kubriko.dispose()
    }
}
