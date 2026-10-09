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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShowcaseEntryFeaturesTest {

    @Test
    fun everyEntryButTheOtherOnesHasADebugMenu() {
        assertFalse((null as ShowcaseEntry?).hasDebugMenu)
        ShowcaseEntry.entries.forEach { assertEquals(it.type != ShowcaseEntryType.OTHER, it.hasDebugMenu, it.name) }
    }

    @Test
    fun onlyDemoAndTestEntriesShowTheInfoButton() {
        assertFalse((null as ShowcaseEntry?).shouldShowInfoButton)
        ShowcaseEntry.entries.forEach {
            assertEquals(it.type == ShowcaseEntryType.DEMO || it.type == ShowcaseEntryType.TEST, it.shouldShowInfoButton, it.name)
        }
    }

    @Test
    fun onlyTheWelcomeAboutAndLicensesScreensShowTheLogo() {
        assertTrue((null as ShowcaseEntry?).shouldShowLogo)
        ShowcaseEntry.entries.forEach {
            assertEquals(it == ShowcaseEntry.ABOUT || it == ShowcaseEntry.LICENSES, it.shouldShowLogo, it.name)
        }
    }
}
