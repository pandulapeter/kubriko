/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

import com.pandulapeter.kubrikoShowcase.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowcaseEntryAvailabilityTest {

    @Test
    fun testEntriesAreAvailableOnlyWithTheTestExamples() = ShowcaseEntry.entries
        .filter { it.type == ShowcaseEntryType.TEST }
        .forEach { assertEquals(BuildConfig.ARE_TEST_EXAMPLES_ENABLED, it.isAvailable, it.name) }

    @Test
    fun unfinishedEntriesAreAvailableOnlyWithTheUnfinishedGames() = ShowcaseEntry.entries
        .filter { !it.isProductionReady }
        .forEach { assertEquals(BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES, it.isAvailable, it.name) }

    @Test
    fun everyOtherEntryIsAvailable() = ShowcaseEntry.entries
        .filter { it.type != ShowcaseEntryType.TEST && it.isProductionReady }
        .forEach { assertEquals(true, it.isAvailable, it.name) }
}
