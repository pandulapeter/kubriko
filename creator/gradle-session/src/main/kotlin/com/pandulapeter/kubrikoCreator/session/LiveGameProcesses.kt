/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The games and agents Kubriko Creator has started and not yet seen end, which a shutdown hook kills, with every process
 * they started, when Creator's JVM exits by any route: a game or an agent would otherwise outlive it. From any thread.
 */
public object LiveGameProcesses {
    private val processes = ConcurrentHashMap.newKeySet<Process>()
    private val isHookInstalled = AtomicBoolean(false)

    /**
     * Kills [process] if Creator exits before it is [remove]d.
     */
    public fun add(process: Process) {
        if (isHookInstalled.compareAndSet(false, true)) Runtime.getRuntime().addShutdownHook(Thread(::killAll, "kill-live-processes"))
        processes += process
    }

    /**
     * Leaves [process] alive when Creator exits.
     */
    public fun remove(process: Process) {
        processes -= process
    }

    /**
     * Whether the process with [pid] is one of them.
     */
    public operator fun contains(pid: Long): Boolean = processes.any { it.pid() == pid }

    /**
     * Kills without waiting, since a shutdown hook has to be quick.
     */
    private fun killAll() {
        processes.forEach { process ->
            try {
                process.descendants().forEach(ProcessHandle::destroyForcibly)
                process.destroyForcibly()
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }
}
