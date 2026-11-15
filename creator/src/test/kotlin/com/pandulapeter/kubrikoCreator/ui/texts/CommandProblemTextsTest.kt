/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.texts

import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandProblemTextsTest {

    /**
     * The problems about a Block's value are handed on to a mapping of their own, which the compiler can't check, so
     * this does.
     */
    @Test
    fun everyProblemHasItsOwnText() {
        val resources = CommandProblem.entries.associateWith { it.toResource() }
        assertEquals(resources.size, resources.values.toSet().size)
    }
}
