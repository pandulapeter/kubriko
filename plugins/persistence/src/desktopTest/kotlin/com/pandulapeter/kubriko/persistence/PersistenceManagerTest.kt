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

class PersistenceManagerTest {

    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoPersistenceTest")
    private val kubriko: ManualKubriko = newManualKubriko(persistenceManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun sameKeyReturnsTheSameFlow() {
        assertSame(persistenceManager.int("score", 0), persistenceManager.int("score", 0))
        assertSame(persistenceManager.string("name", ""), persistenceManager.string("name", ""))
    }

    @Test
    fun valuesStayAtTheirDefaultsUntilTheFirstComposition() {
        assertEquals(true, persistenceManager.boolean("isSoundEnabled", true).value)
        assertEquals(7, persistenceManager.int("score", 7).value)
        assertEquals(0.5f, persistenceManager.float("volume", 0.5f).value)
        assertEquals("default", persistenceManager.string("name", "default").value)
        assertEquals(1 to 2, persistenceManager.generic("position", 1 to 2, serializer = { "" }, deserializer = { 0 to 0 }).value)
    }
}
