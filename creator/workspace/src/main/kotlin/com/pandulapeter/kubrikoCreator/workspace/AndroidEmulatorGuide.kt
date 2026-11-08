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

/**
 * Opens Android's guide to creating emulators in the browser, or gives its address in the console if it can't.
 *
 * @param openBrowser Opens the system's browser on an address, returning whether it could.
 */
internal class AndroidEmulatorGuide(
    private val openBrowser: (URI) -> Boolean,
    private val console: ConsoleLog,
) {
    fun open() {
        if (!openBrowser(ADDRESS)) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.BROWSER_NOT_OPENED, arguments = listOf(ADDRESS.toString())))
        }
    }

    internal companion object {
        val ADDRESS: URI = URI("https://developer.android.com/studio/run/managing-avds")
    }
}
