/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_entry_point_missing
import kubriko.creator.generated.resources.error_entry_point_multiple
import kubriko.creator.generated.resources.error_entry_point_not_loaded
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClassLoaderEntryPointLoaderTest {
    private val root = createTempDirectory().toFile()

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun classesRegistering(vararg classNames: String) = File(root, "classes${root.list().orEmpty().size}").apply {
        File(this, "META-INF/services/com.pandulapeter.kubriko.creator.CreatorEntryPoint").apply { parentFile.mkdirs() }
            .writeText(classNames.joinToString("\n"))
    }

    private fun load(vararg files: File) = ClassLoaderEntryPointLoader.load(ProjectClasspath(kubrikoVersion = "1.0.0", files = files.toList()))

    @Test
    fun theProjectsEntryPointIsFoundThroughItsServicesFile() {
        val loaded = load(classesRegistering(TestEntryPoint::class.java.name))
        assertIs<TestEntryPoint>(loaded.entryPoint)
        loaded.close()
    }

    @Test
    fun classesCreatorAlreadyHasAreShared() {
        val loaded = load(classesRegistering(TestEntryPoint::class.java.name))
        assertEquals(TestEntryPoint::class.java, loaded.entryPoint::class.java)
        loaded.close()
    }

    @Test
    fun aProjectWithoutAnEntryPointIsReported() {
        val exception = assertFailsWith<EntryPointLoadingException> { load(File(root, "empty").apply { mkdirs() }) }
        assertEquals(Res.string.error_entry_point_missing, exception.reason)
    }

    @Test
    fun aProjectWithSeveralEntryPointsIsReported() {
        val exception = assertFailsWith<EntryPointLoadingException> {
            load(classesRegistering(TestEntryPoint::class.java.name), classesRegistering(OtherTestEntryPoint::class.java.name))
        }
        assertEquals(Res.string.error_entry_point_multiple, exception.reason)
    }

    @Test
    fun anEntryPointThatCantBeLoadedIsReported() {
        val exception = assertFailsWith<EntryPointLoadingException> { load(classesRegistering("com.example.Missing")) }
        assertEquals(Res.string.error_entry_point_not_loaded, exception.reason)
    }

    @Test
    fun onlyJarsAndFoldersTheProjectBuildsAreCopied() {
        fun jar(path: String) = File(root, path).apply { parentFile.mkdirs(); writeText("") }
        assertTrue(ClassLoaderEntryPointLoader.isProjectOutput(jar("game/build/libs/game-desktop.jar")))
        assertTrue(ClassLoaderEntryPointLoader.isProjectOutput(File(root, "game/build/classes").apply { mkdirs() }))
        assertFalse(ClassLoaderEntryPointLoader.isProjectOutput(jar(".gradle/caches/modules-2/files-2.1/engine.jar")))
        assertFalse(ClassLoaderEntryPointLoader.isProjectOutput(jar(".m2/repository/io/github/engine.jar")))
        assertFalse(ClassLoaderEntryPointLoader.isProjectOutput(jar("game/build/libs/notes.txt")))
    }

    @Test
    fun aRebuiltFolderDoesntChangeTheClassesLoadedFromItButItsResourcesStayReadable() {
        val classes = File(root, "game/build/classes").apply { mkdirs() }
        val probePath = SnapshotProbe::class.java.name.replace('.', '/') + ".class"
        val classFile = File(classes, probePath).apply {
            parentFile.mkdirs()
            writeBytes(checkNotNull(SnapshotProbe::class.java.classLoader.getResourceAsStream(probePath)).use { it.readBytes() })
        }
        val resource = File(classes, "files/scene.json").apply { parentFile.mkdirs(); writeText("{}") }
        val copies = File(root, "copies").apply { mkdirs() }
        ClassLoaderEntryPointLoader.classLoaderOf(listOf(classes), copies, parent = ClassLoader.getPlatformClassLoader()).use { classLoader ->
            classFile.writeText("rebuilt into something that isn't a class")
            resource.writeText("{ }")
            assertEquals(classLoader, classLoader.loadClass(SnapshotProbe::class.java.name).classLoader)
            assertEquals("{ }", classLoader.getResource("files/scene.json")!!.readText())
            assertTrue(copies.list().orEmpty().isEmpty(), "folders aren't copied")
        }
    }
}

class SnapshotProbe
