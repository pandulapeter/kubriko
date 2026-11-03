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

/**
 * The last lines a build or a tool printed, kept as they come in from any thread, for the console to show when it fails.
 */
internal class OutputTail(
    private val size: Int = DEFAULT_SIZE,
) {
    private val lines = ArrayDeque<String>()

    fun add(line: String) = synchronized(lines) {
        lines.addLast(line)
        if (lines.size > size) lines.removeFirst()
    }

    fun toList(): List<String> = synchronized(lines) { lines.toList() }

    companion object {
        const val DEFAULT_SIZE = 30
    }
}

/**
 * Why a tool failed, in its own words: the line of [output] that says it failed, or its last one, or its exit code.
 */
internal fun failureReasonOf(output: String, exitCode: Int?): String = output.lines().map(String::trim).let { lines ->
    lines.firstOrNull { "Failure" in it || it.startsWith("Error") } ?: lines.lastOrNull { it.isNotEmpty() }
} ?: "exit code $exitCode"

/**
 * [timeoutInMilliseconds] in whole minutes, as the console names a wait, never fewer than one.
 */
internal fun minutesOf(timeoutInMilliseconds: Long): String = maxOf(1L, (timeoutInMilliseconds + 59_999L) / 60_000L).toString()
