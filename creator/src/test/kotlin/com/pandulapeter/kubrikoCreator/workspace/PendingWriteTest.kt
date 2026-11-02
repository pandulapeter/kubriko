/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingWriteTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val ballWithDescription = ActorTypeDefinition(name = "Ball", description = "Bounces.")
    private val paddle = ActorTypeDefinition(name = "Paddle")
    private val game = ManagerTypeDefinition(name = "Game")

    @Test
    fun aSaveFollowedByASaveOfTheSameTypeKeepsOnlyTheLast() {
        assertEquals(
            listOf(PendingWrite.Save(ballWithDescription), PendingWrite.Regenerate),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Save(ball), PendingWrite.Save(ballWithDescription), PendingWrite.Regenerate)),
        )
    }

    @Test
    fun aSaveFollowedByADeleteKeepsOnlyTheDelete() {
        assertEquals(
            listOf(PendingWrite.Delete("Ball")),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Save(ball), PendingWrite.Delete("Ball"))),
        )
    }

    @Test
    fun savesOfOtherTypesAndFolderWritesKeepTheirOrder() {
        val writes = listOf(
            PendingWrite.Save(paddle),
            PendingWrite.CreateFolder(BlockTypeKind.ACTOR, "Bricks"),
            PendingWrite.Save(ball),
            PendingWrite.DeleteFolder(BlockTypeKind.MANAGER, "Old"),
            PendingWrite.Save(game),
            PendingWrite.Overwrite("Ball"),
            PendingWrite.Regenerate,
        )
        assertEquals(
            listOf(
                PendingWrite.Save(paddle),
                PendingWrite.CreateFolder(BlockTypeKind.ACTOR, "Bricks"),
                PendingWrite.Save(ball),
                PendingWrite.DeleteFolder(BlockTypeKind.MANAGER, "Old"),
                PendingWrite.Save(game),
                PendingWrite.Overwrite("Ball"),
                PendingWrite.Regenerate,
            ),
            PendingWrite.withoutOverwrittenSaves(writes),
        )
    }

    @Test
    fun aDeleteFollowedByASaveBecomesAReplacingSaveInTheDeletesPlace() {
        assertEquals(
            listOf(PendingWrite.Save(ballWithDescription, isReplacing = true), PendingWrite.Save(paddle)),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Delete("Ball"), PendingWrite.Save(paddle), PendingWrite.Save(ballWithDescription))),
        )
    }

    @Test
    fun aReplacingSaveSavedAgainStaysReplacing() {
        assertEquals(
            listOf(PendingWrite.Save(ballWithDescription, isReplacing = true)),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Delete("Ball"), PendingWrite.Save(ball), PendingWrite.Save(ballWithDescription))),
        )
    }

    @Test
    fun aDeleteOfATypeAlreadyBeingDeletedIsDropped() {
        assertEquals(
            listOf(PendingWrite.Delete("Ball"), PendingWrite.Save(paddle)),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Delete("Ball"), PendingWrite.Save(paddle), PendingWrite.Delete("Ball"))),
        )
    }

    @Test
    fun aMergedSaveKeepsTheEarlierPlaceWithTheLaterDefinition() {
        assertEquals(
            listOf(PendingWrite.Save(ballWithDescription), PendingWrite.Save(paddle), PendingWrite.Delete("Old")),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Save(ball), PendingWrite.Save(paddle), PendingWrite.Delete("Old"), PendingWrite.Save(ballWithDescription))),
        )
    }

    @Test
    fun aSaveAfterADeleteOfTheSameTypeReplacesItInsteadOfMergingWithTheSaveBeforeIt() {
        assertEquals(
            listOf(PendingWrite.Save(ballWithDescription, isReplacing = true), PendingWrite.Save(paddle)),
            PendingWrite.withoutOverwrittenSaves(listOf(PendingWrite.Save(ball), PendingWrite.Delete("Ball"), PendingWrite.Save(paddle), PendingWrite.Save(ballWithDescription))),
        )
    }
}
