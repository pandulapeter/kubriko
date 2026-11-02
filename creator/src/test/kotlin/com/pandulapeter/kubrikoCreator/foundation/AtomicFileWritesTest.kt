/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicFileWritesTest {
    private val directory = createTempDirectory().toFile()
    private val isPosix = directory.toPath().fileSystem.supportedFileAttributeViews().contains("posix")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun aFileIsReplacedAndNoTemporaryFileStays() {
        val file = File(directory, "Ball.json").apply { writeText("old") }
        file.writeTextAtomically("new")
        assertEquals("new", file.readText())
        assertEquals(listOf("Ball.json"), directory.list()!!.toList())
    }

    @Test
    fun aNewFileAndItsFoldersAreCreatedWithTheUsualPermissions() {
        val file = File(directory, "scenes/levels/1-1.json")
        file.writeTextAtomically("{}")
        assertEquals("{}", file.readText())
        if (isPosix) {
            val plain = File(directory, "plain.json").apply { writeText("{}") }
            assertEquals(Files.getPosixFilePermissions(plain.toPath()), Files.getPosixFilePermissions(file.toPath()))
        }
    }

    @Test
    fun aFileKeepsItsPermissions() {
        if (!isPosix) return
        val file = File(directory, "gradlew").apply { writeText("old") }
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-x---"))
        file.writeTextAtomically("new")
        assertEquals(PosixFilePermissions.fromString("rwxr-x---"), Files.getPosixFilePermissions(file.toPath()))
    }

    @Test
    fun aNewPrivateFileIsReadableByItsOwnerOnlyAndAnExistingOneKeepsItsPermissions() {
        if (!isPosix) return
        val file = File(directory, "mcp.json")
        file.writeTextAtomically("{}", isPrivate = true)
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"))
        file.writeTextAtomically("{ }", isPrivate = true)
        assertEquals(PosixFilePermissions.fromString("rw-r--r--"), Files.getPosixFilePermissions(file.toPath()))
    }

    @Test
    fun aLinkIsFollowedAndStaysALink() {
        val target = File(directory, "real.json").apply { writeText("old") }
        val link = File(directory, "link.json")
        Files.createSymbolicLink(link.toPath(), target.toPath())
        link.writeTextAtomically("new")
        assertTrue(Files.isSymbolicLink(link.toPath()))
        assertEquals("new", target.readText())
    }
}
