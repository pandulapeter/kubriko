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
    fun anythingWithoutAVersionIsRejected() {
        assertNull(ProjectClasspath.parse(""))
        assertNull(ProjectClasspath.parse("/game/build/libs/game.jar"))
    }
}
