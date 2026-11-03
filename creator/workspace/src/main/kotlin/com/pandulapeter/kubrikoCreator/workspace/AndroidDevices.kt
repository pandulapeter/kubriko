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
import java.io.File

/**
 * The Android devices connected to this computer and the emulators it has, found through the SDK's `adb` and `emulator`.
 * Each call runs them, so it is made off the main thread.
 */
internal class AndroidDevices(
    sdk: File,
    private val tools: ToolProcesses,
    isWindows: Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true),
) {
    val adb: String = File(sdk, listOf("platform-tools", if (isWindows) "adb.exe" else "adb").joinToString(File.separator)).path
    val emulator: String = File(sdk, listOf("emulator", if (isWindows) "emulator.exe" else "emulator").joinToString(File.separator)).path

    /**
     * A device `adb` lists.
     *
     * @param state `device` once it can be used, `unauthorized` until its user allows debugging, or `offline`.
     * @param model What it calls itself, if it said.
     * @param avd The emulator's virtual device, for an emulator that said.
     */
    data class Connected(
        val serial: String,
        val state: String,
        val model: String?,
        val avd: String?,
    ) {
        val isReady: Boolean get() = state == READY
    }

    /**
     * Every device and emulator the game can be played on: those connected first, then the emulators that aren't running.
     */
    suspend fun targets(): List<PlayTarget> {
        val connected = connected()
        val running = connected.mapNotNullTo(HashSet()) { it.avd }
        return connected.map { device ->
            if (device.avd != null) emulatorTarget(device.avd) else PlayTarget(id = DEVICE_PREFIX + device.serial, kind = PlayTarget.Kind.ANDROID, name = device.model ?: device.serial)
        } + avds().filter { it !in running }.sorted().map(::emulatorTarget)
    }

    /**
     * The devices and emulators `adb` lists now.
     */
    suspend fun connected(): List<Connected> {
        val result = tools.run(listOf(adb, "devices", "-l"), TIMEOUT_IN_MILLISECONDS)
        if (!result.isSuccessful) return emptyList()
        return result.output.lineSequence().mapNotNull(::parseDevice).toList().map { device ->
            if (device.serial.startsWith(EMULATOR_SERIAL_PREFIX)) device.copy(avd = avdOf(device.serial)) else device
        }
    }

    /**
     * The state `adb` reports for the device with [serial], or null if it isn't connected.
     */
    suspend fun stateOf(serial: String): String? = tools.run(listOf(adb, "-s", serial, "get-state"), TIMEOUT_IN_MILLISECONDS).takeIf { it.isSuccessful }?.output?.trim()

    private suspend fun avdOf(serial: String) = tools.run(listOf(adb, "-s", serial, "emu", "avd", "name"), TIMEOUT_IN_MILLISECONDS)
        .takeIf { it.isSuccessful }
        ?.output
        ?.lineSequence()
        ?.map(String::trim)
        ?.firstOrNull { it.isNotEmpty() && it != "OK" }

    private suspend fun avds(): List<String> {
        if (!File(emulator).isFile) return emptyList()
        val result = tools.run(listOf(emulator, "-list-avds"), TIMEOUT_IN_MILLISECONDS)
        return if (result.isSuccessful) result.output.lineSequence().map(String::trim).filter(avdNamePattern::matches).toList() else emptyList()
    }

    companion object {
        const val DEVICE_PREFIX = "android:"
        const val EMULATOR_PREFIX = "android-emulator:"
        const val READY = "device"
        const val UNAUTHORIZED = "unauthorized"
        private const val EMULATOR_SERIAL_PREFIX = "emulator-"
        private const val TIMEOUT_IN_MILLISECONDS = 10_000L
        private val avdNamePattern = Regex("[A-Za-z0-9._-]+")

        fun emulatorTarget(avd: String) = PlayTarget(id = EMULATOR_PREFIX + avd, kind = PlayTarget.Kind.ANDROID, name = avd.replace('_', ' '), isVirtual = true)

        /**
         * A line of `adb devices -l` (`serial state key:value…`), or null for its header and the daemon's notes.
         */
        fun parseDevice(line: String): Connected? {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 2 || line.startsWith("List of devices") || line.startsWith("*")) return null
            val details = parts.drop(2).mapNotNull { part -> part.split(':', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
            return Connected(serial = parts[0], state = parts[1], model = details["model"]?.replace('_', ' '), avd = null)
        }
    }
}
