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
 * Runs the command-line tools a game is played elsewhere through (`adb`, `emulator`), so that a fake can replay what
 * they printed.
 */
public interface ToolProcesses {

    /**
     * Runs [command] until it ends, stopping it after [timeoutInMilliseconds].
     */
    public suspend fun run(command: List<String>, timeoutInMilliseconds: Long): ToolResult

    /**
     * Runs [command], handing each line it prints to [onLine], until it ends or the caller is cancelled, which stops it.
     *
     * @return Its exit code, or null if it couldn't be started.
     */
    public suspend fun stream(command: List<String>, onLine: (String) -> Unit): Int?

    /**
     * Starts [command] to run on its own, outliving the game and Kubriko Creator, as an emulator does.
     */
    public fun startDetached(command: List<String>): DetachedTool
}

/**
 * How a command [ToolProcesses.run] ran ended.
 *
 * @param exitCode Null if it couldn't be started, or ran out of time.
 * @param output What it printed, its errors included.
 */
public data class ToolResult(
    public val exitCode: Int?,
    public val output: String,
) {
    /**
     * Whether it ended with exit code 0.
     */
    public val isSuccessful: Boolean get() = exitCode == 0
}

/**
 * A command [ToolProcesses.startDetached] started.
 */
public interface DetachedTool {

    /**
     * Its exit code once it ended, or null while it runs.
     */
    public val exitCode: Int?

    /**
     * The last lines it printed.
     */
    public val output: List<String>
}
