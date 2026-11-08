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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenplayOfferTest {
    private val root = testDirectory()
    private val pages = MutableStateFlow(emptyList<String>())
    private val revision = MutableStateFlow(0)

    private fun TestScope.offer() = UnconfinedTestDispatcher(testScheduler).let { dispatcher ->
        ScreenplayOffer(root, pages, revision, CoroutineScope(backgroundScope.coroutineContext + dispatcher), dispatcher)
    }

    @Test
    fun theOfferIsMadeOnlyOnceThePagesAreKnownToBeNone() = runTest {
        val offer = offer()
        assertEquals(false, offer.isShown.value)
        revision.value = 1
        assertEquals(true, offer.isShown.value)
        pages.value = listOf("README")
        assertEquals(false, offer.isShown.value)
    }

    @Test
    fun anAnswerIsFinalAndKeptOnThisComputer() = runTest {
        revision.value = 1
        val offer = offer()
        offer.answer()
        assertEquals(false, offer.isShown.value)
        assertTrue(File(root, "${LocalProjectStore.FOLDER}/screenplay-offer.json").isFile)
        assertEquals(false, offer().isShown.value)
    }
}
