/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.ui.components.FieldError
import com.pandulapeter.kubrikoCreator.workspace.IosDeviceType
import com.pandulapeter.kubrikoCreator.workspace.IosRuntime
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorCreation
import com.pandulapeter.kubrikoCreator.workspace.IosSimulatorOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.new_ios_simulator_name_taken
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewIosSimulatorStateTest {
    private val iPhone = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro", "iPhone 18 Pro")
    private val iPad = IosDeviceType("com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation", "iPad mini (6th generation)")
    private val ios27 = IosRuntime("com.apple.CoreSimulator.SimRuntime.iOS-27-2", "iOS 27.2", "24B8117", setOf(iPhone.identifier, iPad.identifier))
    private val ios18 = IosRuntime("com.apple.CoreSimulator.SimRuntime.iOS-18-2", "iOS 18.2", "22C150", setOf(iPad.identifier))
    private val ready = IosSimulatorOptions.Ready(listOf(iPhone, iPad), listOf(ios27, ios18), takenNames = setOf("iPhone 18 Pro"))
    private val created = mutableListOf<Triple<String, IosDeviceType, IosRuntime>>()
    private var creation: suspend () -> IosSimulatorCreation = { IosSimulatorCreation.Created(PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS)) }
    private var closings = 0

    private fun TestScope.state(read: suspend () -> IosSimulatorOptions = { ready }) = NewIosSimulatorState(
        read = read,
        create = { name, type, runtime ->
            created += Triple(name, type, runtime)
            creation()
        },
        scope = this,
        onCreated = { closings++ },
    ).also {
        it.reload()
        advanceUntilIdle()
    }

    @Test
    fun itStartsAsTheSuggestedDeviceTypeOnItsNewestVersionUnderAFreeName() = runTest {
        val state = state()
        assertEquals(iPhone, state.deviceType)
        assertEquals(ios27, state.runtime)
        assertEquals("iPhone 18 Pro 2", state.name)
        assertTrue(state.canCreate)
    }

    @Test
    fun anotherDeviceTypeKeepsTheVersionIfItRunsAndTheNameFollowsUntilOneIsTyped() = runTest {
        val state = state()
        state.chooseRuntime(ios27)
        state.chooseDeviceType(iPad)
        assertEquals(ios27, state.runtime)
        assertEquals(listOf(ios27, ios18), state.runtimes)
        assertEquals("iPad mini (6th generation)", state.name)
        state.chooseRuntime(ios18)
        state.changeName("Tablet")
        state.chooseDeviceType(iPhone)
        assertEquals(ios27, state.runtime)
        assertEquals("Tablet", state.name)
    }

    @Test
    fun aTakenNameIsRefusedInTheFieldUntilItChanges() = runTest {
        val state = state()
        state.changeName(" IPHONE 18 pro ")
        state.create()
        advanceUntilIdle()
        assertEquals(FieldError(Res.string.new_ios_simulator_name_taken, listOf("IPHONE 18 pro")), state.nameError)
        assertTrue(created.isEmpty())
        state.changeName("Phone")
        assertNull(state.nameError)
    }

    @Test
    fun aPastedNameHasSpacesForItsLineBreaksAndTabs() = runTest {
        val state = state()
        state.changeName("My\r\nPhone\t2\r\n")
        assertEquals("My  Phone 2  ", state.name)
        state.create()
        advanceUntilIdle()
        assertEquals("My  Phone 2  ", created.single().first)
    }

    @Test
    fun aBlankNameCanNotBeCreated() = runTest {
        val state = state()
        state.changeName("   ")
        assertFalse(state.canCreate)
        state.create()
        advanceUntilIdle()
        assertTrue(created.isEmpty())
    }

    @Test
    fun aMadeSimulatorClosesTheDialogAndOneBeingMadeIsNotMadeTwice() = runTest {
        val finish = CompletableDeferred<IosSimulatorCreation>()
        creation = { finish.await() }
        val state = state()
        state.create()
        state.create()
        advanceUntilIdle()
        assertTrue(state.isCreating)
        assertFalse(state.canCreate)
        finish.complete(IosSimulatorCreation.Created(PlayTarget(id = "ios-simulator:U1", kind = PlayTarget.Kind.IOS)))
        advanceUntilIdle()
        assertEquals(listOf(Triple("iPhone 18 Pro 2", iPhone, ios27)), created)
        assertEquals(1, closings)
        assertFalse(state.isCreating)
    }

    @Test
    fun aRefusalOrAThrowStaysOpenSayingWhyUntilTheNextTry() = runTest {
        creation = { IosSimulatorCreation.Refused("Incompatible device") }
        val state = state()
        state.create()
        advanceUntilIdle()
        assertEquals(IosSimulatorCreation.Refused("Incompatible device"), state.failure)
        creation = { throw IOException("simctl vanished") }
        state.create()
        advanceUntilIdle()
        assertEquals(IosSimulatorCreation.Refused("simctl vanished"), state.failure)
        assertEquals(0, closings)
        assertFalse(state.isCreating)
    }

    @Test
    fun aFailedTryReadsTheNamesAgainSinceItMayHaveMadeOneAfterAll() = runTest {
        var options: IosSimulatorOptions = ready
        creation = { IosSimulatorCreation.TimedOut }
        val state = state { options }
        options = ready.copy(takenNames = setOf("iPhone 18 Pro", "iPhone 18 Pro 2"))
        state.create()
        advanceUntilIdle()
        assertEquals(IosSimulatorCreation.TimedOut, state.failure)
        assertEquals(iPhone, state.deviceType)
        state.create()
        advanceUntilIdle()
        assertEquals(FieldError(Res.string.new_ios_simulator_name_taken, listOf("iPhone 18 Pro 2")), state.nameError)
        assertEquals(1, created.size)
    }

    @Test
    fun theNameAsCreateWasPressedIsTheOneMadeWhateverIsTypedMeanwhile() = runTest {
        val finish = CompletableDeferred<IosSimulatorCreation>()
        creation = { finish.await() }
        val state = state()
        state.create()
        advanceUntilIdle()
        state.changeName("Another")
        finish.complete(IosSimulatorCreation.TimedOut)
        advanceUntilIdle()
        assertEquals("iPhone 18 Pro 2", created.single().first)
    }

    @Test
    fun checkingAgainReadsWhatXcodeOffersAfresh() = runTest {
        var options: IosSimulatorOptions = IosSimulatorOptions.NoRuntime
        val state = state { options }
        assertEquals(IosSimulatorOptions.NoRuntime, state.options)
        assertNull(state.deviceType)
        assertFalse(state.canCreate)
        options = ready
        state.reload()
        assertNull(state.options)
        advanceUntilIdle()
        assertEquals(ready, state.options)
        assertEquals(iPhone, state.deviceType)
    }

    @Test
    fun aReadThatThrowsIsUnreadable() = runTest {
        val state = state { throw IOException("xcrun is missing") }
        assertEquals(IosSimulatorOptions.Unreadable("xcrun is missing"), state.options)
    }
}
