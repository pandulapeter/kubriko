/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, EditorMutation::class)
class BlockTypesControllerTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)
    private var savedCount = 0
    private val savedTypeChanges = mutableListOf<Boolean>()
    private val failures = mutableListOf<FailedWrite>()
    private val failedCount get() = failures.size
    private var savedAgainCount = 0
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
        listener = object : BlockTypesListener {
            override fun onSaved(haveTypesChanged: Boolean) {
                savedCount++
                savedTypeChanges += haveTypesChanged
                onSaved.invoke()
            }

            override fun onSaveFailed(failure: FailedWrite) {
                failures += failure
            }

            override fun onSavedAgain() {
                savedAgainCount++
            }

            override fun onFileUnreadable(file: UnreadableBlockTypeFile) {
                unreadableReports += file.path
            }

            override fun onFilesReadableAgain() {
                readableAgainCount++
            }

            override fun onLeftoversRemoved(paths: List<String>) {
                removedLeftoverReports += paths
            }

            override fun onLeftoverChangedByHand(path: String) {
                leftoverChangedByHandReports += path
            }

            override fun onBuildFilesSynced(changes: KubrikoBuildFileChanges) = onBuildFilesSynced.invoke(changes)
        },
        syncBuildFiles = syncBuildFiles,
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
        val controller = createController(FakeBlockTypeRepository(listOf(wall, ball)))
        assertEquals(listOf(ball, wall), controller.actorTypes.value)
        assertTrue(controller.isLoaded.value)
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
        assertTrue(controller.isLoaded.value)
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

    @Test
    fun aFailedSaveKeepsItAndTheRestQueued() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "Bee"
        controller.put(ActorTypeDefinition(name = "Ant"))
        controller.put(ActorTypeDefinition(name = "Bee"))
        controller.put(ActorTypeDefinition(name = "Cat"))
        advance(200)
        assertEquals(listOf("save Ant"), repository.writes)
        assertEquals(listOf(FailedWrite(FailedWrite.Subject.Type("Bee"), "Locked.")), failures)
        assertEquals(listOf("Bee", "Cat"), controller.unsavedNames())
        assertEquals(2, controller.unwrittenCount.value)
        repository.failingNames.clear()
        advance(2_200)
        assertEquals(listOf("save Ant", "save Bee", "save Cat", "regenerate"), repository.writes)
        assertEquals(1, savedAgainCount)
        assertNull(controller.failure.value)
        assertEquals(emptyList(), controller.unsavedNames())
        assertEquals(0, controller.unwrittenCount.value)
    }

    @Test
    fun theSameFailureIsReportedOnce() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "Bee"
        controller.put(ActorTypeDefinition(name = "Bee"))
        advance(200)
        advance(2_200)
        advance(10_200)
        advance(30_200)
        assertEquals(1, failedCount)
        assertEquals(FailedWrite(FailedWrite.Subject.Type("Bee"), "Locked."), controller.failure.value)
        repository.failingNames.clear()
        advance(30_200)
        assertEquals(listOf("save Bee", "regenerate"), repository.writes)
    }

    @Test
    fun aNewChangeRetriesAtOnce() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "Bee"
        controller.put(ActorTypeDefinition(name = "Bee"))
        advance(200)
        repository.failingNames.clear()
        controller.put(wall)
        advance(150)
        assertEquals(listOf("save Bee", "save Wall", "regenerate"), repository.writes)
    }

    @Test
    fun aFailedDeleteIsNotReadBack() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createWritingController(repository)
        repository.failingNames += "Ball"
        controller.delete("Ball")
        advance(200)
        controller.reloadFromDisk()
        advance(200)
        assertNull(controller["Ball"])
        val loadCount = repository.loadCount
        repository.failingNames.clear()
        advance(2_200)
        assertEquals(listOf("delete Ball", "regenerate"), repository.writes)
        assertEquals(loadCount + 1, repository.loadCount)
        assertNull(repository.stored["Ball"])
        assertNull(controller["Ball"])
    }

    @Test
    fun aRenameDeletesTheOldFileLast() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createWritingController(repository)
        repository.failingNames += "Puck"
        controller.rename("Ball", "Puck")
        advance(200)
        assertEquals(emptyList(), repository.writes)
        assertEquals(setOf("Ball"), repository.stored.keys)
        repository.failingNames.clear()
        advance(2_200)
        assertEquals(listOf("save Puck", "delete Ball", "regenerate"), repository.writes)
    }

    @Test
    fun aRenamedTypeEditedAfterItsSaveFailedIsStillWrittenBeforeTheOldFileIsDeleted() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createWritingController(repository)
        repository.failingNames += "Puck"
        controller.rename("Ball", "Puck")
        advance(200)
        controller.put(ActorTypeDefinition(name = "Puck", width = 3f))
        advance(200)
        assertEquals(emptyList(), repository.writes)
        assertEquals(setOf("Ball"), repository.stored.keys)
        repository.failingNames.clear()
        advance(10_200)
        assertEquals(listOf("save Puck", "delete Ball", "regenerate"), repository.writes)
        assertEquals(ActorTypeDefinition(name = "Puck", width = 3f), repository.stored["Puck"])
    }

    @Test
    fun aRenameRolledBackAfterItsSaveFailedKeepsEverySaveBeforeTheDeletesAfterIt() = runTest {
        val brick = ActorTypeDefinition(name = "Brick", parent = "Ball")
        val repository = FakeBlockTypeRepository(listOf(ball, brick))
        val controller = createWritingController(repository)
        repository.failingNames += "Puck"
        controller.rename("Ball", "Puck")
        advance(200)
        repository.failingNames.clear()
        controller.rename("Puck", "Ball")
        advance(200)
        assertEquals(listOf("save Brick", "replace Ball", "delete Puck", "regenerate"), repository.writes)
        assertEquals(listOf(ball, brick), repository.stored.values.sortedBy { it.name })
    }

    @Test
    fun aRenameRolledBackKeepsTheOldFileWhileItsSaveFails() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createWritingController(repository)
        repository.failingNames += "Puck"
        controller.rename("Ball", "Puck")
        advance(200)
        repository.failingNames.clear()
        repository.failingSaveNames += "Ball"
        controller.rename("Puck", "Ball")
        advance(200)
        assertEquals(emptyList(), repository.writes)
        assertEquals(ball, repository.stored["Ball"])
        repository.failingSaveNames.clear()
        advance(10_200)
        assertEquals(listOf("replace Ball", "delete Puck", "regenerate"), repository.writes)
    }

    @Test
    fun aTypeDeletedAndCreatedAgainIsNeverMissingFromTheDiskWhileItsSaveFails() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createWritingController(repository)
        repository.failingSaveNames += "Ball"
        controller.delete("Ball")
        controller.put(wall.copy(name = "Ball"))
        advance(200)
        assertEquals(ball, repository.stored["Ball"])
        repository.failingSaveNames.clear()
        advance(2_200)
        assertEquals(listOf("replace Ball", "regenerate"), repository.writes)
        assertEquals(wall.copy(name = "Ball"), repository.stored["Ball"])
    }

    @Test
    fun aFailedRegenerationIsRetried() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.regenerateFailure = IllegalStateException("Broken.")
        controller.put(ball)
        advance(200)
        assertEquals(listOf("save Ball"), repository.writes)
        assertEquals(listOf(FailedWrite(FailedWrite.Subject.GeneratedCode, "Broken.")), failures)
        assertEquals(1, controller.unwrittenCount.value)
        assertEquals(emptyList(), controller.unsavedNames())
        repository.regenerateFailure = null
        advance(2_200)
        assertEquals(listOf("save Ball", "regenerate"), repository.writes)
        assertEquals(1, savedAgainCount)
    }

    @Test
    fun awaitWritesReturnsWhileBlocked() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "Ball"
        controller.put(ball)
        withTimeout(1_000) { controller.awaitWrites() }
        assertEquals(listOf("Ball"), controller.unsavedNames())
    }

    @Test
    fun aTypeWrittenDuringTheReloadIsNotOverwritten() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository)
        controller.awaitWrites()
        repository.onLoad = {
            repository.onLoad = {}
            controller.put(ball.copy(width = 3f))
        }
        controller.reload()
        controller.awaitWrites()
        assertEquals(ball.copy(width = 3f), controller["Ball"])
        assertEquals(ball.copy(width = 3f), repository.stored["Ball"])
        assertEquals(3, repository.loadCount)
    }

    @Test
    fun aRegenerationQueuedDuringTheReloadDoesNotStopIt() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        repository.stored["Ball"] = ball.copy(width = 7f)
        repository.onLoad = {
            repository.onLoad = {}
            controller.regenerate()
        }
        controller.reload()
        assertEquals(ball.copy(width = 7f), controller["Ball"])
        assertEquals(2, repository.loadCount)
    }

    @Test
    fun anOverwriteBeforeAFailedSaveIsKept() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball)).apply { changedByHand = setOf("Ball") }
        val controller = createWritingController(repository)
        repository.failingNames += "Wall"
        controller.discardChangesMadeByHand("Ball")
        controller.put(wall)
        advance(200)
        assertEquals(emptyList(), repository.writes)
        repository.failingNames.clear()
        advance(2_200)
        assertEquals(listOf("save Wall", "regenerate overwriting Ball"), repository.writes)
        assertEquals(emptySet(), controller.changedByHand.value)
    }

    @Test
    fun aReloadDuringABatchWaitsForIt() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        controller.put(ball.copy(width = 3f))
        var writesWhenAwaited: List<String>? = null
        launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.awaitWrites()
            writesWhenAwaited = repository.writes.toList()
        }
        assertEquals(1, controller.unwrittenCount.value)
        repository.stored["Ball"] = ball
        controller.reloadFromDisk()
        assertEquals(1, repository.loadCount)
        runCurrent()
        assertEquals(ball.copy(width = 3f), controller["Ball"])
        assertEquals(2, repository.loadCount)
        assertEquals(listOf("regenerate", "save Ball", "regenerate"), writesWhenAwaited)
    }

    @Test
    fun awaitWritesSaysWhetherTheChangesAreWritten() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        assertTrue(controller.awaitWrites())
        repository.failingNames += "Ball"
        controller.put(ball)
        assertFalse(controller.awaitWrites())
        assertEquals(UnsavedWrites(subjects = "Ball", reason = "Locked."), controller.unsavedWrites())
        repository.failingNames.clear()
        advance(2_200)
        assertTrue(controller.awaitWrites())
    }

    @Test
    fun awaitWritesSaysTrueThoughAWriteWasQueuedAfterIt() = runTest {
        val repository = FakeBlockTypeRepository()
        var onSaved = {}
        val controller = createController(repository, writeDelayInMilliseconds = 100, onSaved = { onSaved() })
        advance(200)
        controller.put(wall)
        var result: Boolean? = null
        launch { result = controller.awaitWrites() }
        runCurrent()
        onSaved = {
            onSaved = {}
            controller.regenerate()
        }
        advance(200)
        assertEquals(true, result)
    }

    @Test
    fun awaitWrittenWaitsForARetryThatSucceeds() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "Ball"
        controller.put(ball)
        var isWritten = false
        launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.awaitWritten()
            isWritten = true
        }
        advance(1_000)
        assertFalse(isWritten)
        repository.failingNames.clear()
        advance(2_000)
        assertTrue(isWritten)
    }

    @Test
    fun aFolderThatCannotBeChangedIsNamedWhenNoTypeIs() = runTest {
        val repository = FakeBlockTypeRepository()
        val controller = createWritingController(repository)
        repository.failingNames += "enemies"
        controller.createFolder(BlockTypeKind.ACTOR, "enemies")
        assertFalse(controller.awaitWrites())
        assertEquals(UnsavedWrites(subjects = "enemies", reason = "Locked."), controller.unsavedWrites())
        assertEquals(listOf(FailedWrite(FailedWrite.Subject.Folder("enemies"), "Locked.")), failures)
    }

    private fun TestScope.createWritingController(repository: FakeBlockTypeRepository) = createController(repository, writeDelayInMilliseconds = 100).also {
        advance(200)
        repository.writes.clear()
    }

    private fun TestScope.advance(milliseconds: Long) {
        advanceTimeBy(milliseconds)
        runCurrent()
    }
}
