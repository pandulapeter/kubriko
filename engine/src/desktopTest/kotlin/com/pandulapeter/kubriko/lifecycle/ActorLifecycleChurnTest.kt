/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.lifecycle

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.helpers.ManualTickSource
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.Blocker
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.recordingUncaughtExceptions
import org.junit.Assume
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks the actor lifecycle contract (callback pairing, membership, updates only while present, disposal) under a
 * seeded, randomized churn of actor operations. `KUBRIKO_STRESS=1` runs the long versions, which a nightly job runs;
 * a failure message starts with the seed that reproduces it.
 */
class ActorLifecycleChurnTest {

    private object ChurnConfig {
        val isStress = System.getenv("KUBRIKO_STRESS") == "1"
        val singleThreadedSeeds = if (isStress) 1..50 else 1..3
        val blockedSeeds = if (isStress) 1..20 else 1..3
        val tickCount = if (isStress) 5_000 else 400
        val poolSize = if (isStress) 2_000 else 200
        const val CHECK_INTERVAL = 25
        const val GROUP_COUNT = 5
        const val GROUP_SIZE = 4
    }

    private sealed interface Operation {
        val actors: List<LoggingActor>

        data class Add(override val actors: List<LoggingActor>) : Operation
        data class Remove(override val actors: List<LoggingActor>) : Operation
        data object RemoveAll : Operation {
            override val actors = emptyList<LoggingActor>()
        }
    }

    /** The documented semantics, applied one operation at a time in the order the operations were issued. */
    private class Model {
        val members = LinkedHashSet<Int>()
        private val uniqueMembers = HashMap<Class<*>, LoggingActor>()

        fun apply(operation: Operation) {
            when (operation) {
                is Operation.Add -> flatten(operation.actors).forEach { add(it) }
                is Operation.Remove -> flatten(operation.actors).forEach { remove(it) }
                Operation.RemoveAll -> {
                    members.clear()
                    uniqueMembers.clear()
                }
            }
        }

        private fun flatten(actors: List<LoggingActor>) = actors.flatMap { actor ->
            if (actor is Group) listOf(actor) + actor.actors.map { it as LoggingActor } else listOf(actor)
        }

        private fun add(actor: LoggingActor) {
            if (actor.id in members) return
            if (actor is Unique) {
                uniqueMembers.put(actor.javaClass, actor)?.let { members.remove(it.id) }
            }
            members.add(actor.id)
        }

        private fun remove(actor: LoggingActor) {
            if (members.remove(actor.id) && actor is Unique) {
                uniqueMembers.remove(actor.javaClass)
            }
        }
    }

    private class Scene(val log: LifecycleLog, poolSize: Int, idOffset: Int = 0, withUniques: Boolean = true) {
        val pool = List(poolSize) { LoggingActor(idOffset + it, log) }
        val groups = List(ChurnConfig.GROUP_COUNT) { groupIndex ->
            val children = pool.subList(poolSize - (groupIndex + 1) * ChurnConfig.GROUP_SIZE, poolSize - groupIndex * ChurnConfig.GROUP_SIZE)
            LoggingGroup(idOffset + poolSize + groupIndex, log, children)
        }
        val uniques: List<LoggingActor> = if (withUniques) {
            val firstUniqueId = idOffset + poolSize + ChurnConfig.GROUP_COUNT
            List(3) { LoggingUniqueA(firstUniqueId + it, log) } + List(3) { LoggingUniqueB(firstUniqueId + 3 + it, log) }
        } else emptyList()
        val all: List<LoggingActor> get() = pool + groups + uniques

        fun randomOperation(random: Random, allowRemoveAll: Boolean): Operation {
            if (allowRemoveAll && random.nextInt(100) == 0) return Operation.RemoveAll
            val choices = if (uniques.isEmpty()) 4 else 5
            return when (random.nextInt(choices)) {
                0 -> Operation.Add(List(1 + random.nextInt(4)) { pool.random(random) })
                1 -> Operation.Remove(List(1 + random.nextInt(4)) { pool.random(random) })
                2 -> Operation.Add(listOf(groups.random(random)))
                3 -> Operation.Remove(listOf(groups.random(random)))
                else -> Operation.Add(listOf(uniques.random(random)))
            }
        }
    }

    private fun ActorManager.issue(operation: Operation) = when (operation) {
        is Operation.Add -> add(operation.actors)
        is Operation.Remove -> remove(operation.actors)
        Operation.RemoveAll -> removeAll()
    }

    private fun ManualTickSource.tick(log: LifecycleLog) {
        log.currentTick++
        tick(16)
    }

    private fun checkQuiescent(scene: Scene, model: Set<Int>, actorManager: ActorManager, tickSource: ManualTickSource) {
        actorManager.awaitProcessed()
        scene.log.checkInvariants(scene.all, model, actorManager.allActors.value)
        tickSource.tick(scene.log)
        scene.log.checkSingleUpdate(scene.all, model, scene.log.currentTick)
    }

    private fun runChurn(seed: Int, isBlocked: Boolean) {
        val log = LifecycleLog(seed)
        val scene = Scene(log, ChurnConfig.poolSize)
        val model = Model()
        val random = Random(seed)
        val (kubriko, tickSource) = newTestKubriko(actorManager = ActorManager.newInstance(shouldComposeLayers = false))
        val actorManager = kubriko.actorManager
        repeat(ChurnConfig.tickCount) { index ->
            val operations = List(random.nextInt(6)) { scene.randomOperation(random, allowRemoveAll = true) }
            if (isBlocked && index % 10 == 0) {
                val blocker = Blocker()
                actorManager.add(blocker)
                assertTrue(blocker.entered.await(5, TimeUnit.SECONDS), "seed=$seed tick=${log.currentTick} the blocker never ran.")
                operations.forEach { actorManager.issue(it); model.apply(it) }
                blocker.release.countDown()
            } else {
                operations.forEach { actorManager.issue(it); model.apply(it) }
            }
            tickSource.tick(log)
            if (index % ChurnConfig.CHECK_INTERVAL == 0) {
                checkQuiescent(scene, model.members, actorManager, tickSource)
            }
        }
        checkQuiescent(scene, model.members, actorManager, tickSource)
        val members = model.members.toSet()
        val countsBefore = scene.all.associateWith { log.removalCounts(it) }
        kubriko.dispose()
        val eventCountAfterDispose = log.eventCount
        repeat(3) { tickSource.tick(log) }
        assertEquals(eventCountAfterDispose, log.eventCount, "seed=$seed tick=${log.currentTick} events were recorded after dispose() returned.")
        for (actor in scene.all) {
            val (disposedBefore, removedBefore) = countsBefore.getValue(actor)
            val (disposedAfter, removedAfter) = log.removalCounts(actor)
            val expectedDisposed = if (actor.id in members) disposedBefore + 1 else disposedBefore
            assertEquals(expectedDisposed, disposedAfter, "seed=$seed tick=${log.currentTick} actor=${actor.id}: DISPOSED count after dispose()")
            assertEquals(removedBefore, removedAfter, "seed=$seed tick=${log.currentTick} actor=${actor.id}: REMOVED after dispose()")
        }
        log.checkPairing(scene.all, allowTrailingDispose = true)
    }

    @Test
    fun singleThreadedChurnKeepsTheInvariants() = ChurnConfig.singleThreadedSeeds.forEach { runChurn(it, isBlocked = false) }

    @Test
    fun blockedBatchesKeepTheInvariants() = ChurnConfig.blockedSeeds.forEach { runChurn(it, isBlocked = true) }

    @Test
    fun concurrentCallersKeepTheInvariants() {
        Assume.assumeTrue(ChurnConfig.isStress)
        for (seed in 1..5) {
            val log = LifecycleLog(seed)
            val quarterSize = ChurnConfig.poolSize / 4
            val quarterIdSpan = quarterSize + ChurnConfig.GROUP_COUNT
            val scenes = List(4) { Scene(log, quarterSize, idOffset = it * quarterIdSpan, withUniques = false) }
            val models = List(4) { Model() }
            val (kubriko, tickSource) = newTestKubriko(actorManager = ActorManager.newInstance(shouldComposeLayers = false))
            val actorManager = kubriko.actorManager
            val isRunning = AtomicBoolean(true)
            val threads = List(4) { index ->
                thread {
                    val random = Random(seed * 31 + index)
                    while (isRunning.get()) {
                        val operation = scenes[index].randomOperation(random, allowRemoveAll = false)
                        actorManager.issue(operation)
                        models[index].apply(operation)
                        Thread.yield()
                    }
                }
            }
            repeat(ChurnConfig.tickCount / 5) { tickSource.tick(log) }
            isRunning.set(false)
            threads.forEach { it.join() }
            val allActors = scenes.flatMap { it.all }
            val members = models.flatMap { it.members }.toSet()
            actorManager.awaitProcessed()
            log.checkInvariants(allActors, members, actorManager.allActors.value)
            tickSource.tick(log)
            log.checkSingleUpdate(allActors, members, log.currentTick)
            kubriko.dispose()
        }
    }

    @Test
    fun disposeFromAnotherThreadDuringChurn() {
        Assume.assumeTrue(ChurnConfig.isStress)
        for (seed in 1..20) recordingUncaughtExceptions { recorded ->
            val log = LifecycleLog(seed)
            val scene = Scene(log, ChurnConfig.poolSize)
            val random = Random(seed)
            val (kubriko, tickSource) = newTestKubriko(actorManager = ActorManager.newInstance(shouldComposeLayers = false))
            val actorManager = kubriko.actorManager
            val disposeTick = 50 + random.nextInt(200)
            var disposeThread: Thread? = null
            repeat(disposeTick + 50) { index ->
                repeat(random.nextInt(6)) { actorManager.issue(scene.randomOperation(random, allowRemoveAll = true)) }
                if (index == disposeTick) {
                    disposeThread = thread { kubriko.dispose() }
                }
                try {
                    tickSource.tick(log)
                } catch (throwable: Throwable) {
                    fail("seed=$seed tick=${log.currentTick} tick() threw $throwable")
                }
            }
            disposeThread?.join()
            val lastTickBeforeStop = log.currentTick
            repeat(3) { tickSource.tick(log) }
            assertTrue(log.highestUpdateTick <= lastTickBeforeStop, "seed=$seed tick=${log.currentTick} actors were updated after dispose() returned.")
            log.checkPairing(scene.all, allowTrailingDispose = true)
            assertTrue(recorded.isEmpty(), "seed=$seed uncaught: ${recorded.firstOrNull()}")
        }
    }
}
