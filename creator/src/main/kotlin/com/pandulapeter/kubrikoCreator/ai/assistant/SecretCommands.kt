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

import java.io.File

/**
 * The command lines of one platform's secret store, each secret kept under [SERVICE] and its name.
 */
internal sealed interface SecretCommands {
    fun read(name: String): List<String>

    fun write(name: String): List<String>

    /**
     * What [write]'s command reads from its standard input to keep [value].
     */
    fun writeInput(value: String): String

    fun delete(name: String): List<String>

    data object MacKeychain : SecretCommands {
        private const val SECURITY = "/usr/bin/security"

        override fun read(name: String) = listOf(SECURITY, "find-generic-password", "-s", SERVICE, "-a", name, "-w")

        /** Without a value after `-w`, `security` asks for it twice, which it reads from its standard input. */
        override fun write(name: String) = listOf(SECURITY, "add-generic-password", "-U", "-s", SERVICE, "-a", name, "-w")

        override fun writeInput(value: String) = "$value\n$value\n"

        override fun delete(name: String) = listOf(SECURITY, "delete-generic-password", "-s", SERVICE, "-a", name)
    }

    data class SecretService(
        private val secretTool: String,
    ) : SecretCommands {
        override fun read(name: String) = listOf(secretTool, "lookup", "service", SERVICE, "account", name)

        override fun write(name: String) = listOf(secretTool, "store", "--label=$SERVICE: $name", "service", SERVICE, "account", name)

        override fun writeInput(value: String) = value

        override fun delete(name: String) = listOf(secretTool, "clear", "service", SERVICE, "account", name)
    }

    data object CredentialLocker : SecretCommands {
        private const val VAULT = "\$v = New-Object Windows.Security.Credentials.PasswordVault;"
        private const val LOAD = "[void][Windows.Security.Credentials.PasswordVault, Windows.Security.Credentials, ContentType = WindowsRuntime];"

        private fun powerShell(script: String) = listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", LOAD + VAULT + script)

        /** [name] is always one of Kubriko Creator's own constants, so it is safe inside the script's quotes. */
        override fun read(name: String) = powerShell("\$c = \$v.Retrieve('$SERVICE', '$name'); \$c.RetrievePassword(); \$c.Password")

        override fun write(name: String) = powerShell(
            "\$k = [Console]::In.ReadLine(); \$v.Add((New-Object Windows.Security.Credentials.PasswordCredential('$SERVICE', '$name', \$k)))",
        )

        override fun writeInput(value: String) = "$value\n"

        override fun delete(name: String) = powerShell("\$v.Remove(\$v.Retrieve('$SERVICE', '$name'))")
    }

    companion object {
        const val SERVICE = "Kubriko Creator"

        fun forThisPlatform(osName: String = System.getProperty("os.name"), path: String? = System.getenv("PATH")): SecretCommands? = when {
            osName.startsWith("Mac") -> MacKeychain
            osName.startsWith("Windows") -> CredentialLocker
            else -> path.orEmpty().split(File.pathSeparator).map { File(it, "secret-tool") }.firstOrNull { it.canExecute() }?.let { SecretService(it.path) }
        }
    }
}
