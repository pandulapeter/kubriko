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
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ManagerTest {

    private val tickSource = TickSource.manual()
    private val instances = mutableListOf<Kubriko>()

    private fun newInstance(vararg managers: Manager) =
        Kubriko.newInstance(*managers, tickSource = tickSource).also { instances.add(it) }

    @AfterTest
    fun disposeInstances() = instances.forEach { it.dispose() }

    private open class RecordingManager(
        private val name: String = "",
        private val events: MutableList<String> = mutableListOf(),
        isLoggingEnabled: Boolean = false,
    ) : Manager(isLoggingEnabled = isLoggingEnabled) {
        val deltas = mutableListOf<Int>()
        var initialValue = 0
        var lazyInitializations = 0
        val lazyValue by autoInitializingLazy {
            lazyInitializations++
            initialValue * 2
        }

        val exposedScope: CoroutineScope get() = scope

        fun writeLog(message: String) = log(message)

        override fun onInitialize(kubriko: Kubriko) {
            events.add("$name initialized")
            initialValue = 21
        }

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            deltas.add(deltaTimeInMilliseconds)
        }

        override fun onDispose() {
            events.add("$name disposed")
        }
    }

    private class FirstManager(events: MutableList<String>) : RecordingManager("First", events)

    private class SecondManager(events: MutableList<String>) : RecordingManager("Second", events)

    private class LoggingManager : RecordingManager(isLoggingEnabled = true)

    private class StateManagerReadingManager : Manager() {
        var wasRunningDuringInitialization: Boolean? = null

        override fun onInitialize(kubriko: Kubriko) {
            wasRunningDuringInitialization = kubriko.get<StateManager>().isRunning.value
        }
    }

    private class DependentManager : Manager() {
        val dependency by manager<RecordingManager>()
    }

    @Test
    fun customManagersAreInitializedOnStartNotOnCreation() {
        val events = mutableListOf<String>()
        newInstance(RecordingManager("A", events))
        assertEquals(emptyList(), events)

        tickSource.start()

        assertEquals(listOf("A initialized"), events)
    }

    @Test
    fun customManagersAreInitializedInRegistrationOrder() {
        val events = mutableListOf<String>()
        newInstance(SecondManager(events), StateManagerReadingManager(), FirstManager(events))

        tickSource.start()

        assertEquals(listOf("Second initialized", "First initialized"), events)
    }

    @Test
    fun builtInManagersAreUsableFromOnInitialize() {
        val manager = StateManagerReadingManager()
        newInstance(manager)

        tickSource.start()

        assertNotNull(manager.wasRunningDuringInitialization)
    }

    @Test
    fun onUpdateReceivesEveryTickWithItsDelta() {
        val manager = RecordingManager()
        newInstance(manager)
        tickSource.start()

        tickSource.tick(16)
        tickSource.tick(33)

        assertEquals(listOf(16, 33), manager.deltas)
    }

    @Test
    fun onDisposeRunsWhenTheInstanceIsDisposed() {
        val events = mutableListOf<String>()
        val kubriko = newInstance(RecordingManager("A", events))
        tickSource.start()

        kubriko.dispose()

        assertEquals(listOf("A initialized", "A disposed"), events)
    }

    @Test
    fun scopeIsUnavailableBeforeInitialization() {
        val manager = RecordingManager()
        newInstance(manager)

        assertFailsWith<IllegalStateException> { manager.exposedScope }
    }

    @Test
    fun scopeIsTheInstanceScopeAfterInitialization() {
        val manager = RecordingManager()
        val kubriko = newInstance(manager)

        tickSource.start()

        assertSame<Any>(kubriko, manager.exposedScope)
    }

    @Test
    fun managerDelegateResolvesTheRegisteredManager() {
        val dependency = RecordingManager()
        val dependent = DependentManager()
        newInstance(dependent, dependency)

        tickSource.start()

        assertSame(dependency, dependent.dependency)
    }

    @Test
    fun managerDelegateIsUnavailableBeforeInitialization() {
        val dependent = DependentManager()
        newInstance(dependent, RecordingManager())

        assertFailsWith<IllegalStateException> { dependent.dependency }
    }

    @Test
    fun managerDelegateOfAnUnregisteredManagerFailsTheStart() {
        newInstance(DependentManager())

        assertFailsWith<IllegalStateException> { tickSource.start() }
    }

    @Test
    fun autoInitializingLazyRunsOnceRightAfterOnInitialize() {
        val manager = RecordingManager()
        newInstance(manager)
        assertEquals(0, manager.lazyInitializations)

        tickSource.start()

        assertEquals(1, manager.lazyInitializations)
        assertEquals(42, manager.lazyValue)
        assertEquals(1, manager.lazyInitializations)
    }

    @Test
    fun logReachesTheLoggerOnlyWhenLoggingIsEnabled() {
        val silentMessage = "ManagerTest silent ${hashCode()}"
        val loggedMessage = "ManagerTest logged ${hashCode()}"
        val silentManager = RecordingManager(isLoggingEnabled = false)
        val loggingManager = LoggingManager()
        newInstance(silentManager, loggingManager)
        tickSource.start()

        silentManager.writeLog(silentMessage)
        loggingManager.writeLog(loggedMessage)

        val messages = Logger.logs.value.map { it.message }
        assertTrue(loggedMessage in messages)
        assertTrue(silentMessage !in messages)
    }
}
