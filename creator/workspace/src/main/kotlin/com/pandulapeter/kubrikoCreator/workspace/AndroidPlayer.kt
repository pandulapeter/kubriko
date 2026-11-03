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

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays the game on an Android device or emulator: builds the `androidApp` module's debug app while the device gets ready,
 * installs it, starts it with Kubriko Creator's switches, and streams what its process logs into the console until it
 * ends, the device leaves, or it is stopped, which stops the app.
 *
 * @param devices The devices of the Android SDK the project builds with, or null if there is none. Blocks.
 * @param runTask Runs a task by its Gradle path, returning whether it succeeded.
 */
internal class AndroidPlayer(
    private val rootDirectory: File,
    private val devices: () -> AndroidDevices?,
    private val runTask: suspend (taskPath: String) -> Boolean,
    private val tools: ToolProcesses,
    private val console: ConsoleLog,
    private val ioDispatcher: CoroutineDispatcher,
    private val pollIntervalInMilliseconds: Long = 1_000L,
    private val bootTimeoutInMilliseconds: Long = BOOT_TIMEOUT_IN_MILLISECONDS,
) {
    /**
     * Plays the game on [target], starting in [firstScene] (its first Scene when null), calling [onUp] once it runs.
     *
     * @return Whether it ran and ended well; false, as the console says, if it couldn't be played or ended with an error.
     */
    suspend fun play(target: PlayTarget, firstScene: String?, onUp: () -> Unit): Boolean {
        val name = target.name ?: target.id
        val devices = withContext(ioDispatcher) { devices() } ?: run {
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_SDK_MISSING, isError = true))
            return false
        }
        val (isBuilt, serial) = coroutineScope {
            val serial = async { AndroidDeviceStart(devices, tools, console, pollIntervalInMilliseconds, bootTimeoutInMilliseconds).readySerialOf(target) }
            val isBuilt = runTask(BUILD_TASK_PATH)
            if (!isBuilt) serial.cancel()
            isBuilt to if (isBuilt) serial.await() else null
        }
        if (!isBuilt || serial == null) return false
        val app = withContext(ioDispatcher) { AndroidApp.builtIn(File(rootDirectory, AndroidApp.OUTPUT_PATH)) } ?: run {
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_APP_MISSING, isError = true, arguments = listOf(AndroidApp.OUTPUT_PATH)))
            return false
        }
        val shell = listOf(devices.adb, "-s", serial)
        if (!install(shell, app, name)) return false
        val pid = launch(shell, app.applicationId, firstScene, name) ?: return false
        try {
            onUp()
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_GAME_STARTED, arguments = listOf(name)))
            return watch(devices, shell, serial, app.applicationId, pid, name)
        } catch (exception: CancellationException) {
            withContext(NonCancellable) { tools.run(shell + listOf("shell", "am", "force-stop", app.applicationId), COMMAND_TIMEOUT_IN_MILLISECONDS) }
            throw exception
        }
    }

    private suspend fun install(shell: List<String>, app: AndroidApp, name: String): Boolean {
        console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_INSTALLING, arguments = listOf(name)))
        val result = tools.run(shell + listOf("install", "-r", app.file.path), INSTALL_TIMEOUT_IN_MILLISECONDS)
        if (result.isSuccessful && "Failure" !in result.output) return true
        console.append(
            if (INCOMPATIBLE_UPDATE in result.output) {
                ConsoleEntry.Status(WorkspaceStatus.ANDROID_INSTALL_INCOMPATIBLE, isError = true, arguments = listOf(name))
            } else {
                ConsoleEntry.Status(WorkspaceStatus.ANDROID_INSTALL_FAILED, isError = true, arguments = listOf(name, reasonOf(result)))
            },
        )
        return false
    }

    /**
     * Starts the app afresh with Kubriko Creator's switches, returning its process ID, or null, as the console says, if it
     * couldn't be started.
     */
    private suspend fun launch(shell: List<String>, applicationId: String, firstScene: String?, name: String): String? {
        fun failed(result: ToolResult): String? {
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_NOT_STARTED, isError = true, arguments = listOf(name, reasonOf(result))))
            return null
        }
        tools.run(shell + listOf("shell", "am", "force-stop", applicationId), COMMAND_TIMEOUT_IN_MILLISECONDS)
        val resolved = tools.run(shell + listOf("shell", "cmd", "package", "resolve-activity", "--brief", "-c", LAUNCHER_CATEGORY, applicationId), COMMAND_TIMEOUT_IN_MILLISECONDS)
        val activity = resolved.output.lines().map(String::trim).lastOrNull { it.startsWith("$applicationId/") }
        if (!resolved.isSuccessful || activity == null) return failed(resolved)
        val switches = listOf("--ez", ManagerTypeCodeGenerator.LOGGING_PROPERTY, "true") +
            firstScene?.let { listOf("--es", ManagerTypeCodeGenerator.FIRST_SCENE_PROPERTY, shellQuoted(it)) }.orEmpty()
        val started = tools.run(shell + listOf("shell", "am", "start", "-W", "-n", activity) + switches, START_TIMEOUT_IN_MILLISECONDS)
        if (!started.isSuccessful || started.output.lines().any { it.startsWith("Error") }) return failed(started)
        repeat(PID_ATTEMPTS) {
            pidOf(shell, applicationId)?.let { return it }
            delay(pollIntervalInMilliseconds)
        }
        return failed(started)
    }

    /**
     * Streams what the process [pid] logs into the console until it ends or the device leaves.
     *
     * @return Whether it ended well.
     */
    private suspend fun watch(devices: AndroidDevices, shell: List<String>, serial: String, applicationId: String, pid: String, name: String) = coroutineScope {
        val hasCrashed = AtomicBoolean(false)
        val logs = launch {
            tools.stream(shell + listOf("logcat", "-v", "tag", "--pid=$pid")) { line ->
                if (LogcatLines.isCrash(line)) hasCrashed.set(true)
                LogcatLines.entryOf(line)?.let(console::append)
            }
        }
        while (pidOf(shell, applicationId) == pid) delay(pollIntervalInMilliseconds)
        if (devices.stateOf(serial) != AndroidDevices.READY) {
            logs.cancel()
            console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_DEVICE_LEFT, isError = true, arguments = listOf(name)))
            return@coroutineScope false
        }
        // What the process logged last reaches the stream a moment after it ended.
        delay(pollIntervalInMilliseconds)
        logs.cancel()
        if (hasCrashed.get()) console.append(ConsoleEntry.Status(WorkspaceStatus.ANDROID_GAME_CRASHED, isError = true, arguments = listOf(name)))
        !hasCrashed.get()
    }

    private suspend fun pidOf(shell: List<String>, applicationId: String) =
        tools.run(shell + listOf("shell", "pidof", applicationId), COMMAND_TIMEOUT_IN_MILLISECONDS).takeIf { it.isSuccessful }?.output?.trim()?.takeIf { it.isNotEmpty() }

    private fun reasonOf(result: ToolResult) = result.output.lines().map(String::trim).let { lines ->
        lines.firstOrNull { "Failure" in it || it.startsWith("Error") } ?: lines.lastOrNull { it.isNotEmpty() }
    } ?: "exit code ${result.exitCode}"

    internal companion object {
        const val BUILD_TASK_PATH = ":androidApp:assembleDebug"
        const val BOOT_TIMEOUT_IN_MILLISECONDS = 5 * 60_000L
        private const val COMMAND_TIMEOUT_IN_MILLISECONDS = 10_000L
        private const val INSTALL_TIMEOUT_IN_MILLISECONDS = 5 * 60_000L
        private const val START_TIMEOUT_IN_MILLISECONDS = 60_000L
        private const val PID_ATTEMPTS = 10
        private const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
        private const val INCOMPATIBLE_UPDATE = "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

        /**
         * [value] as one argument of the device's shell, which `adb shell` hands its arguments to joined by spaces.
         */
        fun shellQuoted(value: String) = "'" + value.replace("'", "'\\''") + "'"
    }
}
