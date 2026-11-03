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

import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonFileStoreTest {
    private val directory = testDirectory()
    private val file = File(directory, "settings/x.json")
    private val backup = File(directory, "settings/x.json.bak")
    private val serializer = ListSerializer(String.serializer())

    private fun store(isPrivate: Boolean = false) = JsonFileStore(file, serializer, isPrivate)

    private fun writeGarbage() {
        file.parentFile.mkdirs()
        file.writeText("{ not a list")
    }

    @Test
    fun aMissingFileIsMissing() {
        assertEquals(JsonFileStore.Read.Missing, store().read())
    }

    @Test
    fun aValidFileIsFound() {
        assertTrue(store().write(listOf("a", "b")))
        assertEquals(JsonFileStore.Read.Found(listOf("a", "b")), store().read())
    }

    @Test
    fun garbageIsUnreadableWithAReason() {
        writeGarbage()
        val read = assertIs<JsonFileStore.Read.Unreadable>(store().read())
        assertTrue(read.isContentBroken)
        assertTrue(read.reason.isNotBlank())
    }

    @Test
    fun writingOverGarbageSetsItAsideFirst() {
        writeGarbage()
        assertTrue(store().write(listOf("new")))
        assertEquals("{ not a list", backup.readText())
        assertEquals(JsonFileStore.Read.Found(listOf("new")), store().read())
        assertTrue(store().write(listOf("newer")))
        assertEquals("{ not a list", backup.readText())
        assertEquals(JsonFileStore.Read.Found(listOf("newer")), store().read())
    }

    @Test
    fun aPrivateFileIsReadableByItsOwnerOnly() {
        assumeTrue(directory.toPath().fileSystem.supportedFileAttributeViews().contains("posix"))
        assertTrue(store(isPrivate = true).write(listOf("secret")))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file.toPath()))
    }

    @Test
    fun aPrivateFileSetAsideIsReadableByItsOwnerOnly() {
        assumeTrue(directory.toPath().fileSystem.supportedFileAttributeViews().contains("posix"))
        writeGarbage()
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"))
        assertTrue(store(isPrivate = true).write(listOf("secret")))
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(backup.toPath()))
    }

    @Test
    fun nothingIsWrittenWhenTheFileCantBeSetAside() {
        writeGarbage()
        File(backup, "inside").apply { parentFile.mkdirs() }.writeText("taken")
        assertFalse(store().write(listOf("new")))
        assertEquals("{ not a list", file.readText())
    }

    @Test
    fun aFileThatCantBeReadIsntReplaced() {
        assertTrue(store().write(listOf("kept")))
        val bytes = file.readBytes()
        val store = JsonFileStore(file, serializer, readText = { throw IOException("The file is locked.") })
        assertEquals(JsonFileStore.Read.Unreadable("The file is locked.", isContentBroken = false), store.read())
        assertFalse(store.write(listOf("new")))
        assertFalse(backup.exists())
        assertTrue(bytes.contentEquals(file.readBytes()))
    }

    @Test
    fun anUpdateChangesTheValueInTheFile() {
        assertTrue(store().write(listOf("a")))
        assertTrue(store().update { current -> current.orEmpty() + "b" })
        assertEquals(JsonFileStore.Read.Found(listOf("a", "b")), store().read())
    }

    @Test
    fun anUpdateReturningNullLeavesTheFile() {
        assertTrue(store().write(listOf("a")))
        assertFalse(store().update { null })
        assertEquals(JsonFileStore.Read.Found(listOf("a")), store().read())
    }

    @Test
    fun anUpdateOverGarbageStartsFromNothingAndSetsItAside() {
        writeGarbage()
        assertTrue(store().update { current -> current.orEmpty() + "new" })
        assertEquals("{ not a list", backup.readText())
        assertEquals(JsonFileStore.Read.Found(listOf("new")), store().read())
    }

    @Test
    fun anUpdateOfAFileThatCantBeReadIsntMade() {
        assertTrue(store().write(listOf("kept")))
        var isCalled = false
        val store = JsonFileStore(file, serializer, readText = { throw IOException("The file is locked.") })
        val isWritten = store.update {
            isCalled = true
            listOf("new")
        }
        assertFalse(isWritten)
        assertFalse(isCalled)
        assertEquals(JsonFileStore.Read.Found(listOf("kept")), store().read())
    }
}
