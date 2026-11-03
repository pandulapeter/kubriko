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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Gets the Android device or emulator a [PlayTarget] names ready to install the game on: an emulator that isn't running is
 * started and waited for until it has booted, and one that is running is waited for too, while a device has to be
 * connected and allow this computer to debug it.
 */
internal class AndroidDeviceStart(
    private val devices: AndroidDevices,
    private val tools: ToolProcesses,
    private val console: ConsoleLog,
    private val pollIntervalInMilliseconds: Long,
    private val bootTimeoutInMilliseconds: Long,
) {
    /**
     * The serial of the device [target] names once it is ready, or null, as the console says, if it can't be.
     */
    suspend fun readySerialOf(target: PlayTarget): String? {
        val name = target.name ?: target.id
        return when {
            target.id.startsWith(AndroidDevices.EMULATOR_PREFIX) -> bootedEmulator(target.id.removePrefix(AndroidDevices.EMULATOR_PREFIX), name)
            else -> connectedDevice(target.id.removePrefix(AndroidDevices.DEVICE_PREFIX), name)
        }
    }

    private suspend fun connectedDevice(serial: String, name: String): String? {
        val device = devices.connected().firstOrNull { it.serial == serial }
        val problem = when {
            device == null -> ConsoleEntry.Status(WorkspaceStatus.ANDROID_DEVICE_NOT_CONNECTED, isError = true, arguments = listOf(name))
            device.state == AndroidDevices.UNAUTHORIZED -> ConsoleEntry.Status(WorkspaceStatus.ANDROID_DEVICE_UNAUTHORIZED, isError = true, arguments = listOf(name))
            !device.isReady -> ConsoleEntry.Status(WorkspaceStatus.ANDROID_DEVICE_OFFLINE, isError = true, arguments = listOf(name, device.state))
            else -> return serial
        }
        console.append(problem)
        return null
    }

    private suspend fun bootedEmulator(avd: String, name: String): String? {
        val emulator = if (devices.connected().none { it.avd == avd }) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_EMULATOR_STARTING, arguments = listOf(name)))
            tools.startDetached(listOf(devices.emulator, "-avd", avd))
        } else {
            null
        }
        val serial = withTimeoutOrNull(bootTimeoutInMilliseconds) { awaitBooted(avd, emulator) }
        when {
            serial != null -> Unit
            emulator?.exitCode != null -> {
                console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_EMULATOR_FAILED, isError = true, arguments = listOf(name)))
                emulator.output.forEach { console.append(ConsoleEntry.BuildOutput(it)) }
            }
            else -> console.append(
                ConsoleEntry.Status(
                    WorkspaceStatus.ANDROID_EMULATOR_TIMED_OUT,
                    isError = true,
                    arguments = listOf(name, minutesOf(bootTimeoutInMilliseconds)),
                ),
            )
        }
        return serial
    }

    /**
     * The serial of the emulator of [avd] once it has booted, or null if [emulator] ended first.
     */
    private suspend fun awaitBooted(avd: String, emulator: DetachedTool?): String? {
        while (emulator?.exitCode == null) {
            val device = devices.connected().firstOrNull { it.avd == avd }
            if (device?.isReady == true && hasBooted(device.serial)) return device.serial
            delay(pollIntervalInMilliseconds)
        }
        return null
    }

    private suspend fun hasBooted(serial: String) =
        tools.run(listOf(devices.adb, "-s", serial, "shell", "getprop", "sys.boot_completed"), COMMAND_TIMEOUT_IN_MILLISECONDS).output.trim() == "1"

    private companion object {
        const val COMMAND_TIMEOUT_IN_MILLISECONDS = 10_000L
    }
}
