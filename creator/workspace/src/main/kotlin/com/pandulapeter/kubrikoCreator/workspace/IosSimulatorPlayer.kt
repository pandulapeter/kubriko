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
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Plays the game on an iOS simulator: builds the Xcode project in `iosApp/` for it while it boots, installs the app,
 * and starts it with Kubriko Creator's switches as launch arguments, showing what it prints in the console until it
 * ends, the simulator shuts down, or it is stopped, which ends the app.
 *
 * @param simulators This Mac's simulators, or null if it can't build for iOS.
 */
internal class IosSimulatorPlayer(
    private val rootDirectory: File,
    private val simulators: () -> IosSimulators?,
    private val tools: ToolProcesses,
    private val console: ConsoleLog,
    private val ioDispatcher: CoroutineDispatcher,
    private val bootTimeoutInMilliseconds: Long = BOOT_TIMEOUT_IN_MILLISECONDS,
) {
    private val buildDirectory = File(rootDirectory, BUILD_PATH)

    /**
     * Plays the game on [target], starting in [firstScene] (its first Scene when null), calling [onUp] once it runs.
     *
     * @return Whether it ran and ended well; false, as the console says, if it couldn't be played or ended with an error.
     */
    suspend fun play(target: PlayTarget, firstScene: String?, onUp: () -> Unit): Boolean {
        val name = target.name ?: target.id
        val udid = target.id.removePrefix(IosSimulators.PREFIX)
        val simulators = withContext(ioDispatcher) { simulators() } ?: run {
            console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_XCODE_MISSING, isError = true))
            return false
        }
        builtAlongside(build = { build(udid) }, ready = { booted(simulators, udid, name) }) ?: return false
        val app = withContext(ioDispatcher) { buildDirectory.resolve(PRODUCTS_PATH).listFiles().orEmpty().firstOrNull { it.isDirectory && it.extension == "app" } }
        val bundleId = app?.let { bundleIdOf(it) } ?: run {
            console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_APP_MISSING, isError = true, arguments = listOf(File(BUILD_PATH, PRODUCTS_PATH).path)))
            return false
        }
        val installed = tools.run(listOf("xcrun", "simctl", "install", udid, app.path), INSTALL_TIMEOUT_IN_MILLISECONDS)
        if (!installed.isSuccessful) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_INSTALL_FAILED, isError = true, arguments = listOf(name, reasonOf(installed.output, installed.exitCode))))
            return false
        }
        return try {
            launch(simulators, udid, bundleId, firstScene, name, onUp)
        } catch (exception: CancellationException) {
            withContext(NonCancellable) { tools.run(listOf("xcrun", "simctl", "terminate", udid, bundleId), COMMAND_TIMEOUT_IN_MILLISECONDS) }
            throw exception
        }
    }

    /**
     * Builds the app with Xcode, which builds the game's framework through Gradle, showing its errors (or, when it names
     * none, the end of its output) when it fails.
     */
    private suspend fun build(udid: String): Boolean {
        console.append(ConsoleEntry.Status(WorkspaceStatus.BUILDING))
        val tail = ArrayDeque<String>()
        val errors = mutableListOf<String>()
        val command = listOf(
            "xcrun", "xcodebuild", "-project", File(rootDirectory, PROJECT_PATH).path, "-scheme", SCHEME, "-configuration", "Debug",
            "-sdk", "iphonesimulator", "-destination", "id=$udid", "-derivedDataPath", buildDirectory.path, "CODE_SIGNING_ALLOWED=NO", "build",
        )
        val exitCode = tools.stream(command) { line ->
            synchronized(tail) {
                tail.addLast(line)
                if (tail.size > OUTPUT_TAIL_SIZE) tail.removeFirst()
                if ("error:" in line && line.trim() !in errors) errors += line.trim()
            }
        }
        if (exitCode == 0) return true
        console.append(ConsoleEntry.Status(WorkspaceStatus.BUILD_FAILED, isError = true))
        synchronized(tail) { errors.ifEmpty { tail.toList() } }.forEach { console.append(ConsoleEntry.BuildOutput(it)) }
        return false
    }

    /**
     * Boots the simulator if it is shut down and shows it, returning whether it is ready, or false, as the console says.
     */
    private suspend fun booted(simulators: IosSimulators, udid: String, name: String): Boolean? {
        val simulator = simulators.available().firstOrNull { it.udid == udid } ?: run {
            console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_MISSING, isError = true, arguments = listOf(name)))
            return null
        }
        if (!simulator.isBooted) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_BOOTING, arguments = listOf(name)))
            val booted = tools.run(listOf("xcrun", "simctl", "bootstatus", udid, "-b"), bootTimeoutInMilliseconds)
            if (!booted.isSuccessful) {
                val reason = if (booted.exitCode == null) (bootTimeoutInMilliseconds / 60_000).toString() else null
                console.append(
                    if (reason != null) {
                        ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_TIMED_OUT, isError = true, arguments = listOf(name, reason))
                    } else {
                        ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_NOT_BOOTED, isError = true, arguments = listOf(name, reasonOf(booted.output, booted.exitCode)))
                    },
                )
                return null
            }
        }
        tools.run(listOf("open", "-a", "Simulator", "--args", "-CurrentDeviceUDID", udid), COMMAND_TIMEOUT_IN_MILLISECONDS)
        return true
    }

    private suspend fun bundleIdOf(app: File) = tools.run(listOf("/usr/bin/plutil", "-extract", "CFBundleIdentifier", "raw", "-o", "-", File(app, "Info.plist").path), COMMAND_TIMEOUT_IN_MILLISECONDS)
        .takeIf { it.isSuccessful }
        ?.output
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    /**
     * Starts the app, whose output `simctl` prints until it ends, with a line naming its process once it runs.
     *
     * @return Whether it ended well.
     */
    private suspend fun launch(simulators: IosSimulators, udid: String, bundleId: String, firstScene: String?, name: String, onUp: () -> Unit): Boolean {
        val switches = listOf("-" + ManagerTypeCodeGenerator.LOGGING_PROPERTY, "YES") +
            firstScene?.let { listOf("-" + ManagerTypeCodeGenerator.FIRST_SCENE_PROPERTY, it) }.orEmpty()
        var hasStarted = false
        var hasCrashed = false
        val beforeStart = ArrayDeque<String>()
        val processLine = Regex(Regex.escape(bundleId) + """: \d+""")
        val exitCode = tools.stream(listOf("xcrun", "simctl", "launch", "--console-pty", "--terminate-running-process", udid, bundleId) + switches) { raw ->
            val line = raw.trimEnd('\r')
            when {
                // simctl names the app's process once it runs, sometimes after the app's first lines.
                !hasStarted && processLine.matches(line) -> {
                    hasStarted = true
                    onUp()
                    console.append(ConsoleEntry.Status(WorkspaceStatus.IOS_GAME_STARTED, arguments = listOf(name)))
                    beforeStart.forEach { console.append(entryOf(it)) }
                    beforeStart.clear()
                }
                !hasStarted -> beforeStart.addLast(line)
                else -> console.append(entryOf(line))
            }
            if (CRASH_MARKER in line) hasCrashed = true
        }
        val failure = when {
            !hasStarted -> ConsoleEntry.Status(WorkspaceStatus.IOS_NOT_STARTED, isError = true, arguments = listOf(name, reasonOf(beforeStart.joinToString("\n"), exitCode)))
            simulators.available().none { it.udid == udid && it.isBooted } -> ConsoleEntry.Status(WorkspaceStatus.IOS_SIMULATOR_SHUT_DOWN, isError = true, arguments = listOf(name))
            hasCrashed -> ConsoleEntry.Status(WorkspaceStatus.IOS_GAME_CRASHED, isError = true, arguments = listOf(name))
            else -> return true
        }
        console.append(failure)
        return false
    }

    private fun entryOf(line: String) = if (CRASH_MARKER in line) ConsoleEntry.BuildOutput(line) else PrintedLogs.entryOf(line)

    private fun reasonOf(output: String, exitCode: Int?) = output.lines().map(String::trim).lastOrNull { it.isNotEmpty() } ?: "exit code $exitCode"

    internal companion object {
        const val BOOT_TIMEOUT_IN_MILLISECONDS = 5 * 60_000L
        const val PROJECT_PATH = "iosApp/iosApp.xcodeproj"
        const val SCHEME = "iosApp"

        /**
         * Where Xcode builds the app for the simulator, under the project, in the output folder Kubriko Creator's watcher
         * and git leave alone.
         */
        val BUILD_PATH: String = listOf("build", "kubriko-creator", "ios-simulator").joinToString(File.separator)
        val PRODUCTS_PATH: String = listOf("Build", "Products", "Debug-iphonesimulator").joinToString(File.separator)
        private const val COMMAND_TIMEOUT_IN_MILLISECONDS = 10_000L
        private const val INSTALL_TIMEOUT_IN_MILLISECONDS = 2 * 60_000L
        private const val OUTPUT_TAIL_SIZE = 30

        /**
         * What Kotlin/Native prints when the game throws an exception nothing catches.
         */
        private const val CRASH_MARKER = "Uncaught Kotlin exception"
    }
}
