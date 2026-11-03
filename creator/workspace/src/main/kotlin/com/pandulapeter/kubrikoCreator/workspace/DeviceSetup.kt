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
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import java.net.URI

/**
 * Opens where more places of a kind to play on are set up: Android's guide to creating emulators, in the browser, and the
 * app that adds iOS simulators (DeviceHub, which takes the Simulator app's place in Xcode 27, or that app). Blocks.
 *
 * @param openBrowser Opens the system's browser on an address, returning whether it could.
 */
internal class DeviceSetup(
    private val openBrowser: (URI) -> Boolean,
    private val tools: ToolProcesses,
    private val console: ConsoleLog,
) {
    suspend fun open(kind: PlayTarget.Kind) {
        when (kind) {
            PlayTarget.Kind.DESKTOP, PlayTarget.Kind.WEB -> Unit
            PlayTarget.Kind.ANDROID -> if (!openBrowser(ANDROID_EMULATORS_GUIDE)) {
                console.append(ConsoleEntry.Status(WorkspaceStatus.BROWSER_NOT_OPENED, arguments = listOf(ANDROID_EMULATORS_GUIDE.toString())))
            }
            PlayTarget.Kind.IOS -> {
                val opened = IosSimulatorPlayer.SIMULATOR_APPS.any { command -> tools.run(command, COMMAND_TIMEOUT_IN_MILLISECONDS).isSuccessful }
                if (!opened) console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_APPS_MISSING, isError = true))
            }
        }
    }

    internal companion object {
        val ANDROID_EMULATORS_GUIDE: URI = URI("https://developer.android.com/studio/run/managing-avds")
        private const val COMMAND_TIMEOUT_IN_MILLISECONDS = 10_000L
    }
}
