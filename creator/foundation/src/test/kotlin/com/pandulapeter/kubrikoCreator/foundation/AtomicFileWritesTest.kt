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

import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.CopyOption
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        assumeTrue(isPosix)
        val file = File(directory, "gradlew").apply { writeText("old") }
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-x---"))
        file.writeTextAtomically("new")
        assertEquals(PosixFilePermissions.fromString("rwxr-x---"), Files.getPosixFilePermissions(file.toPath()))
    }

    @Test
    fun aNewPrivateFileIsReadableByItsOwnerOnlyAndAnExistingOneKeepsItsPermissions() {
        assumeTrue(isPosix)
        val file = File(directory, "settings.json")
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

    @Test
    fun aMoveRefusedTwiceIsTriedAgain() {
        val (source, target) = sourceAndTarget()
        val sleeps = mutableListOf<Long>()
        var calls = 0
        moveReplacing(source, target, move = { from, to, options ->
            if (++calls <= 2) throw AccessDeniedException(to.toString())
            Files.move(from, to, *options)
        }, sleep = { sleeps += it })
        assertEquals("new", target.toFile().readText())
        assertEquals(listOf(10L, 20L), sleeps)
        assertEquals(listOf("Ball.json"), directory.list()!!.toList())
    }

    @Test
    fun aMoveRefusedFiveTimesFails() {
        val (source, target) = sourceAndTarget()
        val sleeps = mutableListOf<Long>()
        var calls = 0
        assertFailsWith<AccessDeniedException> {
            moveReplacing(source, target, move = { _, to, _ ->
                calls++
                throw AccessDeniedException(to.toString())
            }, sleep = { sleeps += it })
        }
        assertEquals(5, calls)
        assertEquals(listOf(10L, 20L, 40L, 80L), sleeps)
        assertEquals("old", target.toFile().readText())
    }

    @Test
    fun aFailedWriteLeavesNoTemporaryFile() {
        val target = File(directory, "Ball.json").apply { mkdirs() }
        File(target, "child.json").writeText("{}")
        assertFailsWith<IOException> { target.writeTextAtomically("new") }
        assertEquals(listOf("Ball.json"), directory.list()!!.toList())
    }

    @Test
    fun aSharingViolationIsTriedAgain() {
        val (source, target) = sourceAndTarget()
        val sleeps = mutableListOf<Long>()
        var calls = 0
        moveReplacing(source, target, move = { from, to, options ->
            if (++calls == 1) throw FileSystemException(from.toString(), null, "The process cannot access the file because it is being used by another process")
            Files.move(from, to, *options)
        }, sleep = { sleeps += it })
        assertEquals("new", target.toFile().readText())
        assertEquals(listOf(10L), sleeps)
    }

    @Test
    fun otherFailuresAreNotRetried() {
        val (source, target) = sourceAndTarget()
        listOf(IOException("disk full"), NoSuchFileException(source.toString())).forEach { failure ->
            var calls = 0
            val thrown = assertFailsWith<IOException> {
                moveReplacing(source, target, move = { _, _, _ ->
                    calls++
                    throw failure
                }, sleep = { error("Slept after $failure") })
            }
            assertEquals(failure, thrown)
            assertEquals(1, calls)
        }
    }

    @Test
    fun aFileSystemWithoutAtomicMovesFallsBackOnce() {
        val (source, target) = sourceAndTarget()
        val passedOptions = mutableListOf<List<CopyOption>>()
        moveReplacing(source, target, move = { from, to, options ->
            passedOptions += options.toList()
            when (passedOptions.size) {
                1 -> throw AtomicMoveNotSupportedException(from.toString(), to.toString(), null)
                2 -> throw AccessDeniedException(to.toString())
                else -> Files.move(from, to, *options)
            }
        }, sleep = {})
        assertEquals("new", target.toFile().readText())
        assertEquals(3, passedOptions.size)
        assertTrue(StandardCopyOption.ATOMIC_MOVE in passedOptions[0])
        passedOptions.drop(1).forEach { assertFalse(StandardCopyOption.ATOMIC_MOVE in it) }
    }

    private fun sourceAndTarget(): Pair<Path, Path> {
        val source = File(directory, ".Ball.json.test$ATOMIC_WRITE_SUFFIX").apply { writeText("new") }
        val target = File(directory, "Ball.json").apply { writeText("old") }
        return source.toPath() to target.toPath()
    }
}
