/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.commandContext
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class HandWrittenKotlinTest {

    @Test
    fun theKotlinHeldForAChangeIsReadOnceAndLetGoWhenItEnds() = runTest {
        val context = commandContext()
        val file = File(context.project.rootDirectory, "src/commonMain/kotlin/Game.kt").apply { parentFile.mkdirs() }
        file.writeText("val first = 1")
        val whileHeld = context.withKotlinFilesHeld {
            file.writeText("val first = 2")
            context.withKotlinFilesHeld { context.handWrittenKotlin() }
        }
        assertEquals(mapOf("src/commonMain/kotlin/Game.kt" to "val first = 1"), whileHeld)
        assertEquals(mapOf("src/commonMain/kotlin/Game.kt" to "val first = 2"), context.handWrittenKotlin())
    }
}
