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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, EditorMutation::class)
class BlockTypesControllerTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)
    private var savedCount = 0
    private val savedTypeChanges = mutableListOf<Boolean>()
    private var failedCount = 0

    private fun TestScope.createController(
        repository: FakeBlockTypeRepository,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
        syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
        onBuildFilesSynced: (KubrikoBuildFileChanges) -> Unit = {},
        writeDelayInMilliseconds: Long = 0,
    ) = BlockTypesController(
        repository = repository,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        ioDispatcher = ioDispatcher,
        onSaved = {
            savedCount++
            savedTypeChanges += it
        },
        onSaveFailed = { failedCount++ },
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
    fun regeneratingForChangedScenesSaysTheTypesDidNotChange() = runTest {
        val controller = createController(FakeBlockTypeRepository(listOf(wall)))
        controller.put(ball)
        controller.onScenesChanged()
        assertEquals(listOf(true, false), savedTypeChanges.takeLast(2))
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
}
