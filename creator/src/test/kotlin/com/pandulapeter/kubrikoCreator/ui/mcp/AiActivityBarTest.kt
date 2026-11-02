/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.mcp

import com.pandulapeter.kubrikoCreator.commands.AiStep
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiActivityBarTest {
    private fun step(clientName: String?) = AiStep(0, "add_block", null, isRunning = false, clientName = clientName)

    @Test
    fun clientsAreNamedOnlyWhileStepsComeFromMoreThanOne() {
        assertFalse(showsClientNames(listOf(step("claude-code"), step("claude-code"), step(null))))
        assertTrue(showsClientNames(listOf(step("claude-code"), step("cursor"))))
    }
}
