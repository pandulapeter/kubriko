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
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameMapReadingTest {

    @OptIn(EditorMutation::class)
    @Test
    fun theDigestAndFindingsReadTogetherAreTheOnesReadApart() = runTest {
        val context = commandContext(*BreakoutFixture.types.toTypedArray())
        context.scenes.write("level-1", BreakoutFixture.level)
        val (digest, findings) = context.digestAndHealthFindings()
        assertTrue(digest.scenes.isNotEmpty())
        assertEquals(context.projectDigest(), digest)
        assertEquals(context.healthFindings(), findings)
    }
}
