/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.session

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectClasspathTest {

    @Test
    fun theVersionComesFirstAndTheFilesFollowInOrder() {
        assertEquals(
            ProjectClasspath(kubrikoVersion = "1.0.0", files = listOf(File("/game/build/libs/game.jar"), File("/cache/engine.jar"))),
            ProjectClasspath.parse("kubrikoVersion=1.0.0\n/game/build/libs/game.jar\n\n/cache/engine.jar\n"),
        )
    }

    @Test
    fun libraryLinesAreReadAsVersionsAndNotAsFiles() {
        val classpath = ProjectClasspath.parse(
            "kubrikoVersion=1.0.0\n/game/classes\nlibrary.org.jetbrains.kotlin:kotlin-stdlib=2.4.21\nlibrary.org.jetbrains.compose.runtime:runtime-desktop=1.12.2\n",
        )!!
        assertEquals(listOf(File("/game/classes")), classpath.files)
        assertEquals(
            mapOf("org.jetbrains.kotlin:kotlin-stdlib" to "2.4.21", "org.jetbrains.compose.runtime:runtime-desktop" to "1.12.2"),
            classpath.libraries,
        )
    }

    @Test
    fun aFileWithoutLibraryLinesHasNoLibraries() {
        assertEquals(emptyMap(), ProjectClasspath.parse("kubrikoVersion=1.0.0\n/game/build/libs/game.jar\n\n/cache/engine.jar\n")!!.libraries)
    }

    @Test
    fun anythingWithoutAVersionIsRejected() {
        assertNull(ProjectClasspath.parse(""))
        assertNull(ProjectClasspath.parse("/game/build/libs/game.jar"))
    }
}
