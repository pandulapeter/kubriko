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

import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The operating system's own secret store, through its command line tool: the macOS keychain (`security`), the Secret
 * Service on Linux (`secret-tool`, from libsecret), and the Windows Credential Locker (through PowerShell). Secrets go
 * to the tool through its standard input, never its arguments, which other processes can read.
 */
internal class SystemSecretStore(
    private val commands: SecretCommands? = SecretCommands.forThisPlatform(),
    private val timeoutInMilliseconds: Long = TIMEOUT_IN_MILLISECONDS,
) : SecretStore {
    override val isAvailable get() = commands != null

    override fun read(name: String) = commands?.let { run(it.read(name)) }?.takeIf { it.exitCode == 0 }?.output?.trimEnd('\r', '\n')?.ifEmpty { null }

    override fun write(name: String, value: String) = commands?.let { run(it.write(name), it.writeInput(value)) }?.exitCode == 0

    override fun delete(name: String) {
        commands?.let { run(it.delete(name)) }
    }

    private class Outcome(
        val exitCode: Int,
        val output: String,
    )

    private fun run(command: List<String>, input: String? = null): Outcome? = try {
        val process = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        process.outputStream.bufferedWriter().use { writer -> input?.let(writer::write) }
        val output = StringBuilder()
        // Read on a thread of its own, since a tool waiting for an unlock prompt nobody answers never closes its output.
        val reader = Thread { output.append(process.inputStream.bufferedReader().readText()) }.apply { start() }
        if (process.waitFor(timeoutInMilliseconds, TimeUnit.MILLISECONDS)) {
            reader.join()
            Outcome(process.exitValue(), output.toString())
        } else {
            process.destroyForcibly()
            null
        }
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val TIMEOUT_IN_MILLISECONDS = 10_000L
    }
}
