/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import org.junit.Assume.assumeTrue
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectOutputTest {
    private val parent = createTempDirectory().toFile()
    private val root = File(parent, "game").apply { mkdirs() }

    @AfterTest
    fun tearDown() {
        parent.deleteRecursively()
    }

    private fun file(directory: File, path: String) = File(directory, path).apply {
        parentFile.mkdirs()
        writeText("")
    }

    @Test
    fun aFileUnderTheRootIsOwn() {
        assertTrue(ProjectOutput.isOwn(file(root, "game/build/libs/game.jar"), root))
    }

    @Test
    fun aFileOutsideTheRootIsNot() {
        assertFalse(ProjectOutput.isOwn(file(File(parent, "library"), "x.jar"), root))
    }

    @Test
    fun aSiblingWithTheRootAsPrefixIsNot() {
        assertFalse(ProjectOutput.isOwn(file(File(parent, "game2"), "x.jar"), root))
    }

    @Test
    fun aPathThroughDotDotIsResolved() {
        file(parent, "outside/x.jar")
        assertFalse(ProjectOutput.isOwn(File(root, "game/../../outside/x.jar"), root))
    }

    @Test
    fun aSymlinkedRootStillOwnsItsFiles() {
        val jar = file(root, "build/libs/game.jar")
        val link = File(parent, "link")
        val isLinked = try {
            Files.createSymbolicLink(link.toPath(), root.toPath())
            true
        } catch (_: IOException) {
            false
        } catch (_: UnsupportedOperationException) {
            false
        }
        assumeTrue(isLinked)
        assertTrue(ProjectOutput.isOwn(File(link, "build/libs/game.jar"), root))
        assertTrue(ProjectOutput.isOwn(jar, link))
    }
}
