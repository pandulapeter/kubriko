/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.persistence

import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PersistenceManagerExtensionsTest {

    private enum class Difficulty { EASY, NORMAL, HARD }

    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoPersistenceExtensionsTest")
    private val kubriko: ManualKubriko = newManualKubriko(persistenceManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun enumStartsAtItsDefault() {
        assertEquals(Difficulty.NORMAL, persistenceManager.enum("difficulty", Difficulty.NORMAL).value)
    }

    @Test
    fun sameKeyReturnsTheSameFlow() {
        assertSame(persistenceManager.enum("difficulty", Difficulty.NORMAL), persistenceManager.enum("difficulty", Difficulty.NORMAL))
    }

    @Test
    fun storedNameReadsAsItsEntry() {
        assertEquals(Difficulty.HARD, enumEntryNamed(Difficulty.entries.toTypedArray(), "HARD", Difficulty.NORMAL))
    }

    @Test
    fun storedNameMatchingNoEntryReadsAsTheDefault() {
        assertEquals(Difficulty.NORMAL, enumEntryNamed(Difficulty.entries.toTypedArray(), "NIGHTMARE", Difficulty.NORMAL))
        assertEquals(Difficulty.NORMAL, enumEntryNamed(Difficulty.entries.toTypedArray(), "hard", Difficulty.NORMAL))
    }
}
