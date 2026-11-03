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

import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ToolProcesses] replaying what the tools printed, answering each command by its arguments after the tool's path (so
 * `adb -s serial shell pidof app` is `-s serial shell pidof app`): a command's answers are given in turn, the last one
 * again and again, and a command nobody answered fails as an unknown one would.
 */
internal class FakeToolProcesses : ToolProcesses {
    private val answers = ConcurrentHashMap<String, ArrayDeque<ToolResult>>()
    private val streams = ConcurrentHashMap<String, Pair<List<String>, Boolean>>()

    /**
     * Every command run, streamed, or started, in order.
     */
    val commands = CopyOnWriteArrayList<List<String>>()

    /**
     * What [startDetached] returns, and what it does as it starts, such as making an emulator show in the device list.
     */
    var detached = FakeDetachedTool()
    var onDetached: (List<String>) -> Unit = {}

    fun answer(arguments: String, vararg results: ToolResult) {
        answers[arguments] = ArrayDeque(results.toList())
    }

    fun answer(arguments: String, output: String, exitCode: Int = 0) = answer(arguments, ToolResult(exitCode, output))

    /**
     * Makes [stream] of [arguments] print [lines], then run until it is cancelled, or end if [keepsRunning] is false.
     */
    fun stream(arguments: String, lines: List<String>, keepsRunning: Boolean = true) {
        streams[arguments] = lines to keepsRunning
    }

    fun ran(arguments: String) = commands.any { it.drop(1).joinToString(" ") == arguments }

    override suspend fun run(command: List<String>, timeoutInMilliseconds: Long): ToolResult {
        commands += command
        val queue = answers[command.drop(1).joinToString(" ")] ?: return ToolResult(1, "unknown command")
        return synchronized(queue) { if (queue.size > 1) queue.removeFirst() else queue.first() }
    }

    override suspend fun stream(command: List<String>, onLine: (String) -> Unit): Int? {
        commands += command
        val (lines, keepsRunning) = streams[command.drop(1).joinToString(" ")] ?: return 1
        lines.forEach(onLine)
        if (keepsRunning) awaitCancellation()
        return 0
    }

    override fun startDetached(command: List<String>): DetachedTool {
        commands += command
        onDetached(command)
        return detached
    }

    class FakeDetachedTool(
        @Volatile override var exitCode: Int? = null,
        override val output: List<String> = emptyList(),
    ) : DetachedTool

    companion object {
        fun ok(output: String) = ToolResult(0, output)

        fun failed(output: String) = ToolResult(1, output)
    }
}
