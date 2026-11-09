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
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.file.AccessDeniedException
import javax.net.ssl.SSLHandshakeException
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
    fun aNetworkFailureFromTheGradleDaemonIsKnownByTheNameItKeeps() {
        val placeholder = object : RuntimeException("services.gradle.org") {
            override fun toString() = "java.net.UnknownHostException: services.gradle.org"
        }
        assertEquals(TemplateProblem.NOT_CREATED_OFFLINE, CreationFailure.of(directory, RuntimeException("Could not resolve plugin.", placeholder)).reason)
    }

    @Test
    fun failuresThatAWorkingNetworkCausesTooAreNotBlamedOnIt() {
        listOf(SocketException("Too many open files"), SSLHandshakeException("PKIX path building failed")).forEach { cause ->
            val exception = CreationFailure.of(directory, RuntimeException(cause))
            assertEquals(TemplateProblem.NOT_CREATED, exception.reason, cause.toString())
            assertEquals(listOf(cause.message), exception.arguments)
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
            FileNotFoundException("${File(directory, "Game.kt").path} (Permission denied)"),
            FileNotFoundException("${File(directory, "Game.kt").path} (Access is denied)"),
        ).forEach { cause ->
            val exception = CreationFailure.of(directory, cause)
            assertEquals(TemplateProblem.NOT_CREATED_NOT_ALLOWED, exception.reason, cause.toString())
            assertEquals(listOf(parent.absolutePath), exception.arguments)
        }
    }

    @Test
    fun aWriteRefusedOutsideTheProjectNamesTheFolderItWasRefusedIn() {
        val gradleHome = File(parent, ".gradle/wrapper/dists")
        val exception = CreationFailure.of(directory, RuntimeException(AccessDeniedException(File(gradleHome, "gradle.zip").path)))
        assertEquals(TemplateProblem.NOT_CREATED_NOT_ALLOWED, exception.reason)
        assertEquals(listOf(gradleHome.absolutePath), exception.arguments)
    }

    @Test
    fun aRefusalThatNamesNoPathBlamesNoFolder() {
        listOf(IOException("Operation not permitted"), SecurityException("No.")).forEach { cause ->
            assertEquals(TemplateProblem.NOT_CREATED, CreationFailure.of(directory, cause).reason, cause.toString())
        }
    }

    @Test
    fun anythingElseSaysWhatItsRootCauseSaid() {
        val exception = CreationFailure.of(directory, RuntimeException("Could not execute build using connection to Gradle.", IllegalStateException("The template is broken.")))
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
