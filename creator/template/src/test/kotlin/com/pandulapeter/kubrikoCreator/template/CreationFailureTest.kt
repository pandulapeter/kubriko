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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.file.AccessDeniedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CreationFailureTest {
    private val parent = testDirectory()
    private val directory = File(parent, "games/my-game")

    @Test
    fun noNetworkAnywhereInTheCausesIsTold() {
        listOf(
            UnknownHostException("services.gradle.org"),
            RuntimeException("Could not install Gradle distribution.", IOException("Download failed.", ConnectException("Connection refused"))),
        ).forEach { cause ->
            val exception = CreationFailure.of(directory, cause)
            assertEquals(TemplateProblem.NOT_CREATED_OFFLINE, exception.reason, cause.toString())
            assertSame(cause, exception.cause)
        }
    }

    @Test
    fun aFullDiskIsToldOnEverySystem() {
        listOf(IOException("No space left on device"), IOException("There is not enough space on the disk")).forEach { cause ->
            assertEquals(TemplateProblem.NOT_CREATED_DISK_FULL, CreationFailure.of(directory, RuntimeException(cause)).reason)
        }
    }

    @Test
    fun aWriteThatIsntAllowedNamesTheFolderItWasRefusedIn() {
        listOf(
            AccessDeniedException(directory.path),
            FileNotFoundException("${directory.path}/Game.kt (Permission denied)"),
            FileNotFoundException("${directory.path}\\Game.kt (Access is denied)"),
        ).forEach { cause ->
            val exception = CreationFailure.of(directory, cause)
            assertEquals(TemplateProblem.NOT_CREATED_NOT_ALLOWED, exception.reason, cause.toString())
            assertEquals(listOf(parent.absolutePath), exception.arguments)
        }
    }

    @Test
    fun anythingElseSaysWhatTheCauseSaid() {
        val exception = CreationFailure.of(directory, IllegalStateException("The template is broken."))
        assertEquals(TemplateProblem.NOT_CREATED, exception.reason)
        assertEquals(listOf("The template is broken."), exception.arguments)
        assertEquals(listOf("java.lang.IllegalStateException"), CreationFailure.of(directory, IllegalStateException()).arguments)
    }

    @Test
    fun theNearestExistingFolderIsTheOneAWriteWasRefusedIn() {
        assertEquals(parent.absoluteFile, CreationFailure.nearestExisting(directory))
        assertEquals(parent.absoluteFile, CreationFailure.nearestExisting(parent))
    }
}
