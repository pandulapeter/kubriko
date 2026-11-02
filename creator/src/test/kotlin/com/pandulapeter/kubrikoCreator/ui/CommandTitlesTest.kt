/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandTitlesTest {
    private val names = CommandRegistry.editor.commands.map { it.name }

    @Test
    fun everyRegisteredCommandHasATitle() {
        val untitled = names.filter { commandTitle(it) == null }
        assertTrue(untitled.isEmpty(), "Commands without a title in CommandTitles.kt: $untitled")
    }

    @Test
    fun everyTitleBelongsToARegisteredCommand() {
        val unknown = commandTitleNames - names.toSet()
        assertTrue(unknown.isEmpty(), "Titles of commands that aren't registered: $unknown")
        assertNull(commandTitle("no_such_tool"))
    }
}
