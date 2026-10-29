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
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemSecretStoreTest {
    private val directory = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    /**
     * A store whose secret tool is a shell script running [script].
     */
    private fun store(script: String): SystemSecretStore {
        val tool = File(directory, "secret-tool").apply {
            writeText("#!/bin/sh\n$script\n")
            setExecutable(true)
        }
        return SystemSecretStore(SecretCommands.SecretService(tool.absolutePath), timeoutInMilliseconds = 500)
    }

    @Test
    fun whatTheToolPrintsIsTheSecret() {
        if (isWindows) return
        assertEquals("secret", store("echo secret").read("key"))
    }

    @Test
    fun aToolThatNeverAnswersIsGivenUpOn() {
        if (isWindows) return
        val startTime = System.currentTimeMillis()
        assertNull(store("exec sleep 30").read("key"))
        assertTrue(System.currentTimeMillis() - startTime < 10_000)
    }

    private val isWindows get() = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")
}
