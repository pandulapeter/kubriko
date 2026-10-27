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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_actor_type_name_invalid
import kubriko.creator.generated.resources.error_actor_type_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActorTypesControllerTest {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val wall = ActorTypeDefinition(name = "Wall", shape = ActorShape.RECTANGLE)
    private var savedCount = 0
    private var failedCount = 0

    private fun TestScope.createController(
        repository: FakeActorTypeRepository,
        ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ) = ActorTypesController(
        repository = repository,
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        ioDispatcher = ioDispatcher,
        onSaved = { savedCount++ },
        onSaveFailed = { failedCount++ },
    )

    @Test
    fun theStoredActorTypesAreLoadedWhenOpened() = runTest {
        assertEquals(listOf(ball, wall), createController(FakeActorTypeRepository(listOf(wall, ball))).actorTypes.value)
    }

    @Test
    fun creatingAValidNameSavesAndSelectsAnEmptyActorType() = runTest {
        val repository = FakeActorTypeRepository()
        val controller = createController(repository)
        assertNull(controller.create("Player"))
        assertEquals(listOf(ActorTypeDefinition(name = "Player")), controller.actorTypes.value)
        assertEquals("Player", controller.selectedName.value)
        assertEquals(listOf("regenerate", "save Player"), repository.writes)
        assertEquals(2, savedCount)
    }

    @Test
    fun creatingAnInvalidOrTakenNameChangesNothing() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball))
        val controller = createController(repository)
        assertEquals(Res.string.error_actor_type_name_invalid, controller.create("ball game"))
        assertEquals(Res.string.error_actor_type_name_taken, controller.create("BALL"))
        assertEquals(listOf(ball), controller.actorTypes.value)
        assertNull(controller.selectedName.value)
        assertEquals(listOf("regenerate"), repository.writes)
    }

    @Test
    fun updatesShowImmediatelyAndAreWrittenInTheBackground() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        controller.update(ball.copy(width = 10f))
        assertEquals(listOf(ball.copy(width = 10f)), controller.actorTypes.value)
        assertEquals(listOf("regenerate"), repository.writes)
        runCurrent()
        assertEquals(ball.copy(width = 10f), repository.stored["Ball"])
    }

    @Test
    fun writesHappenInTheOrderTheChangesWereMade() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        (1..5).forEach { controller.update(ball.copy(width = it.toFloat())) }
        controller.delete("Ball")
        runCurrent()
        assertEquals(listOf("regenerate") + List(5) { "save Ball" } + "delete Ball", repository.writes)
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun deletingTheSelectedActorTypeClearsTheSelection() = runTest {
        val controller = createController(FakeActorTypeRepository(listOf(ball, wall)))
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
        val repository = FakeActorTypeRepository(listOf(ball))
        createController(repository)
        assertEquals(listOf("regenerate"), repository.writes)
        assertEquals(1, savedCount)
    }

    @Test
    fun aFailedWriteIsReported() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball)).apply { isFailing = true }
        val controller = createController(repository)
        controller.update(ball.copy(width = 1f))
        assertEquals(2, failedCount)
        assertEquals(0, savedCount)
    }

    @Test
    fun actorTypesWhoseCodeWasChangedByHandAreReportedAndCanBeDiscarded() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball)).apply { changedByHand = setOf("Ball") }
        val controller = createController(repository)
        assertEquals(setOf("Ball"), controller.changedByHand.value)
        controller.discardChangesMadeByHand("Ball")
        assertEquals("regenerate overwriting Ball", repository.writes.last())
        assertEquals(emptySet(), controller.changedByHand.value)
    }

    @Test
    fun definitionsChangedOnDiskAreReloadedAndTheirCodeRegenerated() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball, wall))
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
        val repository = FakeActorTypeRepository(listOf(ball))
        val controller = createController(repository)
        controller.update(ball.copy(width = 3f))
        controller.reloadFromDisk()
        assertEquals(listOf("regenerate", "save Ball"), repository.writes)
        assertEquals(listOf(ball.copy(width = 3f)), controller.actorTypes.value)
    }

    @Test
    fun reloadingWaitsForCreatorsOwnPendingWrites() = runTest {
        val repository = FakeActorTypeRepository(listOf(ball))
        val controller = createController(repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        controller.update(ball.copy(width = 3f))
        repository.stored["Ball"] = ball.copy(width = 99f)
        controller.reloadFromDisk()
        runCurrent()
        assertEquals(listOf(ball.copy(width = 3f)), controller.actorTypes.value)
    }
}
