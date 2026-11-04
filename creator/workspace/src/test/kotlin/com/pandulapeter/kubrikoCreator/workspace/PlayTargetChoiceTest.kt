/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlayTargetChoiceTest {
    private val directory = testDirectory()
    private val file = File(directory, "${LocalProjectStore.FOLDER}/${PlayTargetChoice.FILE_NAME}")
    private val device = PlayTarget(id = "device", kind = PlayTarget.Kind.DESKTOP, name = "Device")
    private var devices = listOf(device)

    @Test
    fun theDesktopIsChosenUntilTheUserChoosesElsewhere() = runTest {
        val choice = choice()
        assertEquals(PlayTargets(listOf(PlayTarget.DESKTOP, device), PlayTarget.DESKTOP), choice.targets.value)
        choice.choose(device)
        assertEquals(PlayTargets(listOf(PlayTarget.DESKTOP, device), device), choice.targets.value)
        assertEquals("""{"target":"device"}""", file.readText().filterNot { it.isWhitespace() })
    }

    @Test
    fun theChoiceIsKeptForTheNextTime() = runTest {
        choice().run {
            refresh()
            choose(device)
        }
        assertEquals(device, choice().targets.value.chosen)
    }

    @Test
    fun aChosenPlaceThatIsGoneMeansTheDesktopUntilItIsBack() = runTest {
        val choice = choice()
        choice.refresh()
        choice.choose(device)
        devices = emptyList()
        assertEquals(PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP), choice.refresh())
        devices = listOf(device)
        assertEquals(device, choice.refresh().chosen)
    }

    @Test
    fun choosingTheDesktopAgainForgetsTheDevice() = runTest {
        val choice = choice()
        choice.refresh()
        choice.choose(device)
        choice.choose(PlayTarget.DESKTOP)
        assertEquals("{}", file.readText().filterNot { it.isWhitespace() })
        assertEquals(PlayTarget.DESKTOP, choice().refresh().chosen)
    }

    @Test
    fun aSourceThatFailsIsLeftOutAndTheOthersAreListed() = runTest {
        val choice = choice(sources = listOf({ error("No adb") }, { FoundPlaces(PlayTarget.Kind.DESKTOP, devices) }))
        assertEquals(listOf(PlayTarget.DESKTOP, device), choice.refresh().available)
    }

    @Test
    fun aKindIsConfiguredWhileItsSourceSaysSoEvenWithNoPlaceThere() = runTest {
        var android = FoundPlaces(PlayTarget.Kind.ANDROID, emptyList())
        val choice = choice(sources = listOf({ android }, { FoundPlaces.NONE }))
        assertEquals(PlayTargets(listOf(PlayTarget.DESKTOP), PlayTarget.DESKTOP, setOf(PlayTarget.Kind.DESKTOP, PlayTarget.Kind.ANDROID)), choice.refresh())
        android = FoundPlaces.NONE
        assertEquals(setOf(PlayTarget.Kind.DESKTOP), choice.refresh().configured)
        assertEquals(setOf(PlayTarget.Kind.DESKTOP), choice.targets.value.configured)
    }

    @Test
    fun aFileThatCantBeReadMeansTheDesktop() = runTest {
        file.parentFile.mkdirs()
        file.writeText("{ not json")
        assertEquals(PlayTarget.DESKTOP, choice().refresh().chosen)
    }

    private fun TestScope.choice(sources: List<suspend () -> FoundPlaces> = listOf({ FoundPlaces(PlayTarget.Kind.DESKTOP, devices) })): PlayTargetChoice {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return PlayTargetChoice(directory, sources, CoroutineScope(backgroundScope.coroutineContext + Job() + dispatcher), dispatcher)
    }
}
