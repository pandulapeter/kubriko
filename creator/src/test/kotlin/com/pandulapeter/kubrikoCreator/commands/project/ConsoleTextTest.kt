/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsoleTextTest {

    @Test
    fun aStatusReadsAsItsEnglishWithItsArguments() {
        assertEquals(
            buildJsonObject {
                put("kind", JsonPrimitive("error"))
                put("text", JsonPrimitive("Level 1 still names the Scene intro, which is now opening. Change it there by hand."))
            },
            ConsoleEntry.Status(CommandProblem.SCENE_NAMED_IN_CODE, isError = true, arguments = listOf("Level 1", "intro", "opening")).toJson(),
        )
    }
}
