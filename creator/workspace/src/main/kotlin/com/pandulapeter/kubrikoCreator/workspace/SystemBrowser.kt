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

import java.awt.Desktop
import java.io.IOException
import java.net.URI

/**
 * The system's browser, which the game plays in when it is played on the web. Blocks.
 */
internal object SystemBrowser {

    /**
     * The system property that, set to [NOWHERE], keeps the browser closed, since the end-to-end checks load the page
     * themselves.
     */
    const val PROPERTY = "kubriko.creator.browser"
    const val NOWHERE = "none"

    /**
     * Opens [address] in the system's browser (through `xdg-open` where Java can't), returning whether it could.
     */
    fun open(address: URI): Boolean {
        if (System.getProperty(PROPERTY) == NOWHERE) return true
        return try {
            when {
                Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) -> Desktop.getDesktop().browse(address)
                System.getProperty("os.name").startsWith("Linux") -> ProcessBuilder("xdg-open", address.toString()).start()
                else -> return false
            }
            true
        } catch (_: IOException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
