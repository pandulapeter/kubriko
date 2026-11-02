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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeFileProblem
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.UnreadableBlockTypeFile
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class, EditorMutation::class)
class BlockTypesControllerTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)
    private var savedCount = 0
    private val savedTypeChanges = mutableListOf<Boolean>()
    private var failedCount = 0
    private val unreadableReports = mutableListOf<String>()
    private var readableAgainCount = 0
    private val removedLeftoverReports = mutableListOf<List<String>>()
    private val leftoverChangedByHandReports = mutableListOf<String>()
    private val brokenFile = UnreadableBlockTypeFile(".kubriko/actors/Brick.json", BlockTypeFileProblem.Unparsable("Unexpected JSON token at offset 12"))

    private fun TestScope.createController(
        repository: FakeBlockTypeRepository,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
        syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
        onBuildFilesSynced: (KubrikoBuildFileChanges) -> Unit = {},
        writeDelayInMilliseconds: Long = 0,
        onSaved: () -> Unit = {},
        onUnexpectedProblem: CoroutineContext = EmptyCoroutineContext,
    ) = BlockTypesController(
        repository = repository,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler) + onUnexpectedProblem),
        ioDispatcher = ioDispatcher,
        onSaved = {
            savedCount++
            savedTypeChanges += it
            onSaved()
        },
        onSaveFailed = { failedCount++ },
        onFileUnreadable = { unreadableReports += it.path },
        onFilesReadableAgain = { readableAgainCount++ },
        onLeftoversRemoved = { removedLeftoverReports += it },
        onLeftoverChangedByHand = { leftoverChangedByHandReports += it },
        syncBuildFiles = syncBuildFiles,
        onBuildFilesSynced = onBuildFilesSynced,
        writeDelayInMilliseconds = writeDelayInMilliseconds,
    )

    @Test
    fun theBuildFilesAreSyncedAfterEveryWriteButNotAfterAFailedOne() = runTest {
        val repository = FakeBlockTypeRepository(listOf(wall))
        val synced = mutableListOf<KubrikoBuildFileChanges>()
        var syncCount = 0
        val controller = createController(
            repository = repository,
            syncBuildFiles = { KubrikoBuildFileChanges(version = "1.0.${syncCount++}") },
            onBuildFilesSynced = { synced += it },
        )
        controller.put(ball)
        assertEquals(listOf("1.0.0", "1.0.1"), synced.map { it.version })
        repository.isFailing = true
        controller.put(ball.copy(width = 5f))
        assertEquals(2, syncCount)
    }

    @Test
    fun leftoverCodeIsReportedWhenRemovedAndOnceWhileKeptForChangesMadeByHand() = runTest {
        val repository = FakeBlockTypeRepository(listOf(wall))
        val controller = createController(repository)
        repository.removedLeftovers = listOf("generated/Ball.kt")
        repository.leftoversChangedByHand = setOf("generated/Brick.kt")
        controller.regenerate()
        repository.removedLeftovers = emptyList()
        controller.regenerate()
        repository.leftoversChangedByHand = setOf("generated/Brick.kt", "generated/Bat.kt")
        controller.regenerate()
        repository.leftoversChangedByHand = emptySet()
        controller.regenerate()
        repository.leftoversChangedByHand = setOf("generated/Brick.kt")
        controller.regenerate()
        assertEquals(listOf(listOf("generated/Ball.kt")), removedLeftoverReports)
        assertEquals(listOf("generated/Brick.kt", "generated/Bat.kt", "generated/Brick.kt"), leftoverChangedByHandReports)
    }

    @Test
    fun regeneratingForChangedScenesSaysTheTypesDidNotChange() = runTest {
        val controller = createController(FakeBlockTypeRepository(listOf(wall)))
        controller.put(ball)
        controller.regenerate()
        assertEquals(listOf(true, false), savedTypeChanges.takeLast(2))
    }

    @Test
    fun regeneratingWhenOpenedSaysTheTypesDidNotChange() = runTest {
        createController(FakeBlockTypeRepository(listOf(wall)))
        assertEquals(listOf(false), savedTypeChanges)
    }

    @Test
    fun theStoredActorTypesAreLoadedWhenOpened() = runTest {
        assertEquals(listOf(ball, wall), createController(FakeBlockTypeRepository(listOf(wall, ball))).actorTypes.value)
    }

    @Test
    fun puttingAnActorTypeAddsOrReplacesItInNameOrder() = runTest {
        val repository = FakeBlockTypeRepository(listOf(wall))
        val controller = createController(repository)
        controller.put(ball)
        controller.put(ball.copy(width = 5f))
        assertEquals(listOf(ball.copy(width = 5f), wall), controller.actorTypes.value)
        assertEquals(ball.copy(width = 5f), controller["Ball"])
        assertEquals(listOf("regenerate", "save Ball", "regenerate", "save Ball", "regenerate"), repository.writes)
    }

    @Test
    fun renamingUpdatesCollisionEventsAndKeepsTheSelection() = runTest {
        val hitsBall = wall.copy(events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Ball")))
        val repository = FakeBlockTypeRepository(listOf(ball, hitsBall))
        val controller = createController(repository)
        controller.select("Ball")
        controller.rename("Ball", "Puck")
        assertEquals(listOf(ball.copy(name = "Puck"), hitsBall.copy(events = listOf(EventDefinition(EventType.COLLISION, otherActorType = "Puck")))), controller.actorTypes.value)
        assertEquals("Puck", controller.selectedName.value)
        assertEquals(setOf("Puck", "Wall"), repository.stored.keys)
    }

    @Test
    fun updatesShowImmediatelyAndAreWrittenInTheBackground() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        controller.put(ball.copy(width = 10f))
        assertEquals(listOf(ball.copy(width = 10f)), controller.actorTypes.value)
        assertEquals(listOf("regenerate"), repository.writes)
        runCurrent()
        assertEquals(ball.copy(width = 10f), repository.stored["Ball"])
    }

    @Test
    fun changesMadeTogetherAreWrittenAsOneBatchInOrderWithoutSavesThatLaterOnesUndo() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        (1..5).forEach { controller.put(ball.copy(width = it.toFloat())) }
        controller.put(wall)
        controller.delete("Ball")
        runCurrent()
        // The first save is already being written when the others come, which make up the next batch.
        assertEquals(listOf("regenerate", "save Ball", "regenerate", "save Wall", "delete Ball", "regenerate"), repository.writes)
        assertEquals(listOf("Wall"), repository.stored.keys.toList())
    }

    @Test
    fun changesWithinTheWriteDelayOfEachOtherAreWrittenTogether() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, writeDelayInMilliseconds = 100)
        advanceTimeBy(200)
        runCurrent()
        (1..10).forEach { width ->
            controller.put(ball.copy(width = width.toFloat()))
            advanceTimeBy(10)
        }
        assertEquals(listOf("regenerate"), repository.writes)
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("regenerate", "save Ball", "regenerate"), repository.writes)
        assertEquals(ball.copy(width = 10f), repository.stored["Ball"])
        controller.awaitWrites()
    }

    @Test
    fun deletingTheSelectedActorTypeClearsTheSelection() = runTest {
        val controller = createController(FakeBlockTypeRepository(listOf(ball, wall)))
        controller.select("Ball")
        controller.delete("Ball")
        assertEquals(listOf(wall), controller.actorTypes.value)
        assertNull(controller.selectedName.value)
        controller.select("Wall")
        controller.delete("Ball")
        assertEquals("Wall", controller.selectedName.value)
    }

    @Test
    fun theGeneratedCodeIsBroughtUpToDateWhenOpened() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        createController(repository)
        assertEquals(listOf("regenerate"), repository.writes)
        assertEquals(1, savedCount)
    }

    @Test
    fun aFailedWriteIsReported() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { isFailing = true }
        val controller = createController(repository)
        controller.put(ball.copy(width = 1f))
        assertEquals(2, failedCount)
        assertEquals(0, savedCount)
    }

    @Test
    fun actorTypesWhoseCodeWasChangedByHandAreReportedAndCanBeDiscarded() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { changedByHand = setOf("Ball") }
        val controller = createController(repository)
        assertEquals(setOf("Ball"), controller.changedByHand.value)
        controller.discardChangesMadeByHand("Ball")
        assertEquals("regenerate overwriting Ball", repository.writes.last())
        assertEquals(emptySet(), controller.changedByHand.value)
    }

    @Test
    fun definitionsChangedOnDiskAreReloadedAndTheirCodeRegenerated() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball, wall))
        val controller = createController(repository)
        controller.select("Wall")
        repository.stored.remove("Wall")
        repository.stored["Ball"] = ball.copy(width = 7f)
        controller.reloadFromDisk()
        assertEquals(listOf(ball.copy(width = 7f)), controller.actorTypes.value)
        assertNull(controller.selectedName.value)
        assertEquals(listOf("regenerate", "regenerate"), repository.writes)
    }

    @Test
    fun reloadingWhatCreatorJustWroteChangesNothing() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository)
        controller.put(ball.copy(width = 3f))
        controller.reloadFromDisk()
        assertEquals(listOf("regenerate", "save Ball", "regenerate"), repository.writes)
        assertEquals(listOf(ball.copy(width = 3f)), controller.actorTypes.value)
    }

    @Test
    fun reloadingWaitsForCreatorsOwnPendingWrites() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        controller.put(ball.copy(width = 3f))
        repository.stored["Ball"] = ball.copy(width = 99f)
        controller.reloadFromDisk()
        runCurrent()
        assertEquals(listOf(ball.copy(width = 3f)), controller.actorTypes.value)
    }

    @Test
    fun aChangeOnDiskWhileCreatorsOwnWritesArePendingIsReadOnceTheyAreWritten() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, writeDelayInMilliseconds = 100)
        advanceTimeBy(200)
        controller.put(wall)
        repository.stored["Brick"] = ActorTypeDefinition(name = "Brick")
        controller.reloadFromDisk()
        assertEquals(listOf("Ball", "Wall"), controller.actorTypes.value.map { it.name })
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("Ball", "Brick", "Wall"), controller.actorTypes.value.map { it.name })
        assertEquals(listOf("regenerate", "save Wall", "regenerate", "regenerate"), repository.writes)
    }

    @Test
    fun managerTypesAreKeptApartFromActorTypes() = runTest {
        val game = ManagerTypeDefinition("Game")
        val controller = createController(FakeBlockTypeRepository(listOf(wall, game, ball)))
        assertEquals(listOf(ball, game, wall), controller.types.value)
        assertEquals(listOf(ball, wall), controller.actorTypes.value)
        assertEquals(listOf(game), controller.managerTypes.value)
        assertEquals(game, controller.managerType("Game"))
        assertNull(controller.actorType("Game"))
        assertNull(controller.managerType("Ball"))
    }

    @Test
    fun renamingAManagerTypeUpdatesEveryTypeThatUsesIt() = runTest {
        val game = ManagerTypeDefinition("Game", properties = listOf(PropertyDefinition.Number("score")))
        val brick = ActorTypeDefinition("Brick", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.ChangeNumber("score", manager = "Game")))))
        val repository = FakeBlockTypeRepository(listOf(game, brick, ball))
        val controller = createController(repository)
        controller.select("Game")
        controller.rename("Game", "Match")
        assertEquals(listOf("Brick", "Match", "Ball").sorted(), controller.types.value.map { it.name })
        assertEquals(BlockDefinition.ChangeNumber("score", manager = "Match"), controller.actorType("Brick")!!.events.single().blocks.single())
        assertEquals("Match", controller.selectedName.value)
        assertEquals(listOf("save Brick", "save Match", "delete Game"), repository.writes.filter { it != "regenerate" })
    }

    @Test
    fun anUnreadableFileIsReportedOnceAndStopsRegeneratingUntilItIsFixed() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { unreadableFiles = listOf(brokenFile) }
        val controller = createController(repository)
        controller.awaitWrites()
        assertEquals(listOf(ball), controller.types.value)
        assertEquals(listOf(brokenFile), controller.unreadableFiles.value)
        assertEquals(listOf("Ball", "Brick"), controller.takenNames)
        controller.put(ball.copy(width = 5f))
        controller.awaitWrites()
        assertEquals(listOf("save Ball"), repository.writes)
        controller.reloadFromDisk()
        assertEquals(listOf(brokenFile.path), unreadableReports)
        repository.unreadableFiles = emptyList()
        controller.reloadFromDisk()
        controller.awaitWrites()
        assertEquals(listOf("save Ball", "regenerate"), repository.writes)
        assertEquals(1, readableAgainCount)
        assertEquals(0, failedCount)
    }

    @Test
    fun aLoadThatFailsKeepsTheWritesGoingWithoutRegenerating() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { loadFailure = IllegalStateException("Broken.") }
        val controller = createController(repository)
        controller.awaitWrites()
        controller.put(wall)
        controller.awaitWrites()
        assertEquals(listOf("save Wall"), repository.writes)
        assertEquals(1, unreadableReports.size)
    }

    @Test
    fun anUnexpectedFailureWhileWritingIsAFailedSaveAndLaterWritesStillHappen() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { regenerateFailure = IllegalStateException("Unexpected.") }
        val controller = createController(repository)
        controller.awaitWrites()
        assertEquals(1, failedCount)
        repository.regenerateFailure = null
        controller.put(wall)
        controller.awaitWrites()
        assertEquals(listOf("save Wall", "regenerate"), repository.writes)
    }

    @Test
    fun aChangeOnDiskWhileCreatorsOwnWritesArePendingIsReadEvenWhenReportingTheWriteFails() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(
            repository = repository,
            writeDelayInMilliseconds = 100,
            onSaved = { if (savedCount == 2) throw IllegalStateException("Unexpected.") },
            onUnexpectedProblem = CoroutineExceptionHandler { _, _ -> },
        )
        advanceTimeBy(200)
        controller.put(wall)
        repository.stored["Brick"] = ActorTypeDefinition(name = "Brick")
        controller.reloadFromDisk()
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("Ball", "Brick", "Wall"), controller.actorTypes.value.map { it.name })
    }

    @Test
    fun aFailureAfterWritingDoesNotStopLaterWrites() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val reported = mutableListOf<Throwable>()
        val controller = createController(
            repository = repository,
            onSaved = { if (savedCount == 1) throw IllegalStateException("Unexpected.") },
            onUnexpectedProblem = CoroutineExceptionHandler { _, exception -> reported += exception },
        )
        withTimeout(1_000) { controller.awaitWrites() }
        controller.put(wall)
        withTimeout(1_000) { controller.awaitWrites() }
        assertEquals(listOf("regenerate", "save Wall", "regenerate"), repository.writes)
        assertEquals(listOf("Unexpected."), reported.map { it.message })
    }
}
