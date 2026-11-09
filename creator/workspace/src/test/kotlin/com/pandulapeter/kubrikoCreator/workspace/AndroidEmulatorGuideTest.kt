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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidEmulatorGuideTest {
    private val console = ConsoleLog()
    private val opened = mutableListOf<URI>()
    private var canOpenBrowser = true

    private val statuses get() = console.lines.value.entries.filterIsInstance<ConsoleEntry.Status>().map { it.text }

    private fun guide() = AndroidEmulatorGuide(
        openBrowser = { address ->
            opened += address
            canOpenBrowser
        },
        console = console,
    )

    @Test
    fun theGuideOpensInTheBrowserOrTheConsoleGivesItsAddress() {
        guide().open()
        canOpenBrowser = false
        guide().open()
        assertEquals(listOf(AndroidEmulatorGuide.ADDRESS, AndroidEmulatorGuide.ADDRESS), opened)
        assertEquals(listOf(WorkspaceStatus.BROWSER_NOT_OPENED), statuses)
    }
}
