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

import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KubrikoTest {

    private class PlainManager : Manager()

    private class UnregisteredManager : Manager()

    private class LifecycleManager : Manager() {
        var disposals = 0
        lateinit var job: Job

        override fun onInitialize(kubriko: Kubriko) {
            job = scope.coroutineContext.job
        }

        override fun onDispose() {
            disposals++
        }
    }

    private fun newStartedInstance(vararg managers: Manager): Kubriko {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(*managers, tickSource = tickSource)
        tickSource.start()
        return kubriko
    }

    @Test
    fun defaultManagersAreCreatedWhenNotProvided() {
        val kubriko = Kubriko.newInstance(tickSource = TickSource.manual())
        try {
            kubriko.get<ActorManager>()
            kubriko.get<MetadataManager>()
            kubriko.get<StateManager>()
            kubriko.get<ViewportManager>()
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun firstManagerOfAClassWins() {
        val first = PlainManager()
        val kubriko = Kubriko.newInstance(first, PlainManager(), tickSource = TickSource.manual())
        try {
            assertSame(first, kubriko.get<PlainManager>())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun providedBuiltInManagerReplacesTheDefault() {
        val viewportManager = ViewportManager.newInstance(initialScaleFactor = 3f)
        val kubriko = Kubriko.newInstance(viewportManager, tickSource = TickSource.manual())
        try {
            assertSame(viewportManager, kubriko.get<ViewportManager>())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun lookupOfAnUnregisteredManagerThrows() {
        val kubriko = Kubriko.newInstance(tickSource = TickSource.manual())
        try {
            assertFailsWith<IllegalStateException> { kubriko.get<UnregisteredManager>() }
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun disposedInstanceRejectsManagerLookup() {
        val kubriko = newStartedInstance()

        kubriko.dispose()

        assertFailsWith<IllegalStateException> { kubriko.get<ActorManager>() }
    }

    @Test
    fun disposeDisposesTheManagersOnce() {
        val manager = LifecycleManager()
        val kubriko = newStartedInstance(manager)

        kubriko.dispose()
        kubriko.dispose()

        assertEquals(1, manager.disposals)
    }

    @Test
    fun disposeCancelsTheCoroutineScope() {
        val manager = LifecycleManager()
        val kubriko = newStartedInstance(manager)

        kubriko.dispose()

        assertTrue(manager.job.isCancelled)
    }
}
