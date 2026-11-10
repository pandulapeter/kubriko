/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * A sign-in command running in the assistant panel's terminal, such as `claude auth login`, which opens the browser and
 * may ask for a code pasted from it. Most programs that sign in this way work without a terminal device, so plain pipes
 * do, and the output is shown as [TerminalText]; one that needs a device runs in the system's terminal instead
 * ([isInSystemTerminal]), whose window shows it.
 *
 * @param scope Where the command's output is read: the main thread, which the panel's state is read on.
 */
public class SignInSession(
    /**
     * The sign-in method the command runs for.
     */
    public val method: SignInMethod,
    private val channel: AgentChannel,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    /**
     * Whether the command runs in the system's terminal ([SystemTerminalLauncher]), so that nothing it prints reaches
     * [output] and nothing can be typed into it from here.
     */
    public val isInSystemTerminal: Boolean = false,
) {
    private val scope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val raw = StringBuilder()
    private val _output = MutableStateFlow(TerminalText("", emptyList()))
    /**
     * What the command printed so far.
     */
    public val output: StateFlow<TerminalText> = _output.asStateFlow()
    private val _exitCode = MutableStateFlow<Int?>(null)

    /**
     * How the command ended, or null while it runs.
     */
    public val exitCode: StateFlow<Int?> = _exitCode.asStateFlow()
    private val _needsTerminalDevice = MutableStateFlow(false)

    /**
     * Whether the command failed saying it needs a terminal device ([TerminalDeviceErrors]), so the system's terminal
     * should run it instead.
     */
    public val needsTerminalDevice: StateFlow<Boolean> = _needsTerminalDevice.asStateFlow()
    private val writer = channel.toAgent.bufferedWriter()

    init {
        this.scope.launch {
            listOf(read(channel.fromAgent), read(channel.errors)).joinAll()
            val exitCode = withContext(ioDispatcher) { channel.awaitExit() }
            _needsTerminalDevice.value = exitCode != 0 && TerminalDeviceErrors.areIn(raw.toString())
            _exitCode.value = exitCode
        }
    }

    private fun read(stream: InputStream) = scope.launch {
        val buffer = CharArray(BUFFER_SIZE)
        val reader = stream.reader()
        while (true) {
            val count = try {
                withContext(ioDispatcher) { reader.read(buffer) }
            } catch (_: IOException) {
                -1
            }
            if (count < 0) break
            raw.append(buffer, 0, count)
            if (raw.length > MAXIMUM_OUTPUT) raw.delete(0, raw.length - MAXIMUM_OUTPUT)
            _output.value = TerminalText.of(raw.toString())
        }
    }

    /**
     * Types [line] into the command, followed by Enter.
     */
    public fun send(line: String) {
        if (_exitCode.value != null) return
        scope.launch(ioDispatcher) {
            try {
                writer.write(line)
                writer.newLine()
                writer.flush()
            } catch (_: IOException) {
                // The command ended meanwhile, which exitCode shows.
            }
        }
    }

    /**
     * Ends the command, if it still runs.
     */
    internal suspend fun stop() {
        withContext(ioDispatcher) { channel.stop() }
        scope.cancel()
    }

    /**
     * Ends the command before returning, when Kubriko Creator quits. It blocks until the command is stopped, so it is
     * called off the main thread.
     */
    internal fun stopNow() {
        channel.stop()
        scope.cancel()
    }

    private companion object {
        const val BUFFER_SIZE = 1024
        const val MAXIMUM_OUTPUT = 32_768
    }
}
