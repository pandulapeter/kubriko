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

import com.pandulapeter.kubrikoCreator.session.LiveGameProcesses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * [ToolProcesses] as processes of this computer, each killed when Kubriko Creator exits but those started detached.
 */
internal object SystemToolProcesses : ToolProcesses {
    private const val OUTPUT_TAIL_SIZE = 30

    /**
     * Its output goes to a file rather than a pipe, since a process it starts that keeps running (the server the first
     * `adb` starts) would hold a pipe open, and reading it, past the timeout.
     */
    override suspend fun run(command: List<String>, timeoutInMilliseconds: Long): ToolResult = withContext(Dispatchers.IO) {
        val log = File.createTempFile("kubriko-creator-tool", ".log")
        try {
            val process = try {
                ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start()
            } catch (exception: IOException) {
                return@withContext ToolResult(exitCode = null, output = exception.message ?: exception.toString())
            }
            LiveGameProcesses.add(process)
            val hasEnded = try {
                runInterruptible { process.waitFor(timeoutInMilliseconds, TimeUnit.MILLISECONDS) }.also { if (!it) process.destroyForcibly() }
            } catch (exception: CancellationException) {
                process.destroyForcibly()
                throw exception
            } finally {
                LiveGameProcesses.remove(process)
            }
            ToolResult(exitCode = if (hasEnded) process.exitValue() else null, output = log.readText())
        } finally {
            log.delete()
        }
    }

    override suspend fun stream(command: List<String>, onLine: (String) -> Unit): Int? = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (_: IOException) {
            return@withContext null
        }
        LiveGameProcesses.add(process)
        try {
            coroutineScope {
                val reading = launch {
                    try {
                        process.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
                    } catch (_: IOException) {
                    }
                }
                try {
                    runInterruptible { process.waitFor() }
                } catch (exception: CancellationException) {
                    process.destroyForcibly()
                    throw exception
                }
                reading.join()
                process.exitValue()
            }
        } finally {
            LiveGameProcesses.remove(process)
        }
    }

    /**
     * Its output goes to a file rather than a pipe, which would end it once Kubriko Creator exits.
     */
    override fun startDetached(command: List<String>): DetachedTool {
        val log = File.createTempFile("kubriko-creator-tool", ".log").apply { deleteOnExit() }
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start()
        } catch (exception: IOException) {
            log.delete()
            return object : DetachedTool {
                override val exitCode = -1
                override val output = listOf(exception.message ?: exception.toString())
            }
        }
        return object : DetachedTool {
            override val exitCode get() = if (process.isAlive) null else process.exitValue()
            override val output get() = runCatching { log.readLines().takeLast(OUTPUT_TAIL_SIZE) }.getOrDefault(emptyList())
        }
    }
}
