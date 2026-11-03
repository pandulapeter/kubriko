/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * What this computer can build a game's targets with.
 */
public interface TargetHost {

    /**
     * The Android SDK the project in [rootDirectory] would build with, or null if there is none.
     */
    public fun androidSdk(rootDirectory: File): File?

    /**
     * Whether this computer can build for iOS: a Mac with Xcode.
     */
    public val canBuildIos: Boolean

    public companion object {
        /**
         * This computer, as its environment and files tell.
         */
        public val System: TargetHost = SystemTargetHost(
            environment = java.lang.System.getenv(),
            userHome = File(java.lang.System.getProperty("user.home")),
            osName = java.lang.System.getProperty("os.name"),
            applicationsDirectory = File("/Applications"),
            selectedDeveloperDirectory = ::xcodeSelectPath,
        )

        /**
         * The developer folder `xcode-select` picked, which is an Xcode's wherever it is installed, or null if it can't tell.
         */
        private fun xcodeSelectPath(): String? = runCatching {
            val process = ProcessBuilder("/usr/bin/xcode-select", "-p").redirectErrorStream(true).start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                null
            } else {
                process.inputStream.bufferedReader().readText().trim().takeIf { process.exitValue() == 0 }
            }
        }.getOrNull()
    }
}

/**
 * [TargetHost] from an environment: the SDK of the project's `local.properties`, then `ANDROID_HOME` and
 * `ANDROID_SDK_ROOT`, then where Android Studio installs it; and Xcode in the applications folder, `DEVELOPER_DIR`, or the one
 * `xcode-select` picked.
 */
internal class SystemTargetHost(
    private val environment: Map<String, String>,
    private val userHome: File,
    private val osName: String,
    private val applicationsDirectory: File,
    private val selectedDeveloperDirectory: () -> String? = { null },
) : TargetHost {
    private val isMac get() = osName.startsWith("Mac", ignoreCase = true)

    override fun androidSdk(rootDirectory: File): File? = sequence {
        localSdk(rootDirectory)?.let { yield(it) }
        environment["ANDROID_HOME"]?.let { yield(File(it)) }
        environment["ANDROID_SDK_ROOT"]?.let { yield(File(it)) }
        yield(
            when {
                isMac -> File(userHome, "Library/Android/sdk")
                osName.startsWith("Windows", ignoreCase = true) -> File(environment["LOCALAPPDATA"] ?: File(userHome, "AppData/Local").path, "Android/Sdk")
                else -> File(userHome, "Android/Sdk")
            },
        )
    }.firstOrNull(::isSdk)

    override val canBuildIos: Boolean
        get() = isMac && (
            environment["DEVELOPER_DIR"]?.contains(".app/") == true ||
                applicationsDirectory.listFiles().orEmpty().any { it.isDirectory && it.name.startsWith("Xcode") && it.name.endsWith(".app") } ||
                selectedDeveloperDirectory()?.contains(".app/") == true
            )

    private fun localSdk(rootDirectory: File) = File(rootDirectory, LOCAL_PROPERTIES).takeIf { it.isFile }?.let { file ->
        runCatching { Properties().apply { file.reader().use(::load) }.getProperty(SDK_DIRECTORY_KEY) }.getOrNull()
    }?.let(::File)

    companion object {
        const val LOCAL_PROPERTIES = "local.properties"

        /**
         * Whether [directory] holds an Android SDK the build can use.
         */
        fun isSdk(directory: File) = File(directory, "platforms").isDirectory
        const val SDK_DIRECTORY_KEY = "sdk.dir"
    }
}
