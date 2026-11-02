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

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.ChangeLog
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangesSummaryTest {

    @Test
    fun othersChangesAreSummedUpButTheConversationsOwnAreNot() = runTest {
        val log = ChangeLog()
        log.record("add_block", CommandTarget.BlockType("Ball", 2, 1))
        log.record("add_block", CommandTarget.BlockType("Ball", 2, 2), origin = AiStepOrigin("mine"), isByAi = true)
        log.record("rename_scene", CommandTarget.Scene("level"), step = ChangeRecord.Step.UNDO)
        log.record("add_block", null, isByAi = true)
        assertEquals(
            "Changed in the editor since your last turn, by the user unless marked, oldest first: add_block at Ball#2.1; undid rename_scene at " +
                "Scene level; (another AI client) add_block. Read again what you need; the read tools say what changed since you last read it.",
            ChangesSummary.of(log.since(0), since = 0, conversationId = "mine"),
        )
        assertNull(ChangesSummary.of(log.since(4), since = 4, conversationId = "mine"))
        assertNull(ChangesSummary.of(log.since(1).take(1), since = 1, conversationId = "mine"))
    }

    @Test
    fun changesNoLongerKeptAreCounted() = runTest {
        val log = ChangeLog(capacity = 2)
        repeat(5) { log.record("add_block", null) }
        assertEquals(5, log.revision)
        assertEquals(
            "Changed in the editor since your last turn, by the user unless marked, oldest first: 3 earlier changes; add_block; add_block. " +
                "Read again what you need; the read tools say what changed since you last read it.",
            ChangesSummary.of(log.since(0), since = 0, conversationId = null),
        )
    }
}
