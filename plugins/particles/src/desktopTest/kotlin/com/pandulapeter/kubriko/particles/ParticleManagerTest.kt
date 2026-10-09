/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.particles.implementation.ParticleBatch
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticleManagerTest {

    private var kubriko: ManualKubriko? = null

    @AfterTest
    fun tearDown() {
        kubriko?.dispose()
    }

    @Test
    fun burstEmitsItsCountOnceAndTurnsTheEmitterInactive() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(5))
        kubriko.actorManager.add(emitter)

        kubriko.tickUntil { emitter.emittedCount > 0 }
        kubriko.tick(count = 5)

        assertEquals(5, emitter.emittedCount)
        assertEquals(ParticleEmitter.Mode.Inactive, emitter.particleEmissionMode)
    }

    @Test
    fun continuousEmissionCarriesFractionalParticlesAcrossTicks() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Continuous { 0.25f })
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil(deltaTimeInMilliseconds = 10) { emitter.emittedCount > 0 }
        val emittedBefore = emitter.emittedCount

        kubriko.tick(deltaTimeInMilliseconds = 10, count = 4)

        assertEquals(10, emitter.emittedCount - emittedBefore)
    }

    @Test
    fun inactiveEmitterEmitsNothing() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Inactive)
        val probe = TestEmitter(ParticleEmitter.Mode.Burst(1))
        kubriko.actorManager.add(emitter, probe)

        kubriko.tickUntil { probe.emittedCount > 0 }
        kubriko.tick(count = 5)

        assertEquals(0, emitter.emittedCount)
    }

    @Test
    fun liveParticlesAreUpdatedEveryTickUntilTheyFinish() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(1), lifetimeInMilliseconds = 50)
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil { emitter.emittedCount > 0 }
        val particle = emitter.createdStates.single()

        kubriko.tick(count = 10)

        assertEquals(listOf(16, 16, 16, 16), particle.ageDeltas)
    }

    @Test
    fun finishedParticleStatesAreReusedInsteadOfCreated() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(5), lifetimeInMilliseconds = 50)
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil { emitter.emittedCount > 0 }
        kubriko.tick(count = 5)

        emitter.particleEmissionMode = ParticleEmitter.Mode.Burst(5)
        kubriko.tick()

        assertEquals(5, emitter.createdStates.size)
        assertEquals(5, emitter.reusedCount)
    }

    @Test
    fun cacheSizeLimitsHowManyFinishedStatesAreKept() {
        val kubriko = newParticleKubriko(cacheSize = 3)
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(5), lifetimeInMilliseconds = 50)
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil { emitter.emittedCount > 0 }
        kubriko.tick(count = 5)

        emitter.particleEmissionMode = ParticleEmitter.Mode.Burst(5)
        kubriko.tick()

        assertEquals(3, emitter.reusedCount)
        assertEquals(7, emitter.createdStates.size)
    }

    @Test
    fun continuousEmissionStaggersParticlesAcrossTheTick() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Continuous { 0.05f })

        kubriko.actorManager.add(emitter)
        kubriko.tickUntil(deltaTimeInMilliseconds = 100) { emitter.emittedCount > 0 }

        val preAges = emitter.createdStates.map { it.ageDeltas.single() }
        assertTrue(preAges.size > 1, "Pre-ages: $preAges")
        assertTrue(preAges.all { it in 1 until 100 }, "Pre-ages: $preAges")
        assertEquals(preAges.sortedDescending(), preAges)
        assertEquals(preAges.size, preAges.toSet().size)
    }

    @Test
    fun particleWhoseLifetimeFitsInTheCatchUpWindowIsNeverUpdatedAgain() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Continuous { 0.05f }, lifetimeInMilliseconds = 1)
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil(deltaTimeInMilliseconds = 100) { emitter.emittedCount > 0 }
        emitter.particleEmissionMode = ParticleEmitter.Mode.Inactive

        kubriko.tick(count = 3)

        assertTrue(emitter.createdStates.all { it.ageDeltas.size == 1 })
    }

    @Test
    fun burstParticlesAreNotPreAged() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(5))
        kubriko.actorManager.add(emitter)

        kubriko.tickUntil(deltaTimeInMilliseconds = 100) { emitter.emittedCount > 0 }

        assertTrue(emitter.createdStates.all { it.ageDeltas.isEmpty() })
    }

    @Test
    fun pausedSceneNeitherEmitsNorAgesParticles() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Continuous { 0.1f })
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil { emitter.emittedCount > 0 }
        val stateManager = kubriko.kubriko.get<StateManager>()
        stateManager.updateIsRunning(false)
        awaitCondition { !stateManager.isRunning.value }
        val emittedBefore = emitter.emittedCount
        val ageDeltasBefore = emitter.createdStates.map { it.ageDeltas.size }

        kubriko.tick(count = 10)

        assertEquals(emittedBefore, emitter.emittedCount)
        assertEquals(ageDeltasBefore, emitter.createdStates.map { it.ageDeltas.size })
    }

    @Test
    fun removeAllDropsLiveParticlesAndTheirStatesAreReused() {
        val kubriko = newParticleKubriko()
        val emitter = TestEmitter(ParticleEmitter.Mode.Burst(3))
        kubriko.actorManager.add(emitter)
        kubriko.tickUntil { emitter.emittedCount > 0 }

        kubriko.actorManager.removeAll()
        kubriko.actorManager.awaitProcessed()
        kubriko.tickUntil { kubriko.actorManager.allActors.value.any { it is ParticleBatch } }
        val ageDeltasAfterReset = emitter.createdStates.map { it.ageDeltas.size }
        kubriko.tick(count = 5)
        assertEquals(ageDeltasAfterReset, emitter.createdStates.map { it.ageDeltas.size })

        val newEmitter = TestEmitter(ParticleEmitter.Mode.Burst(3))
        kubriko.actorManager.add(newEmitter)
        kubriko.tickUntil { newEmitter.emittedCount > 0 }
        assertEquals(3, newEmitter.reusedCount)
        assertEquals(0, newEmitter.createdStates.size)
    }

    @Test
    fun particleBatchIsReAddedExactlyOnceAfterRemoveAll() {
        val kubriko = newParticleKubriko()
        val actorManager = kubriko.actorManager
        actorManager.add(TestEmitter(ParticleEmitter.Mode.Continuous { 1f }))
        kubriko.tickUntil { actorManager.allActors.value.any { it is ParticleBatch } }

        actorManager.removeAll()
        actorManager.awaitProcessed()
        assertTrue(actorManager.allActors.value.isEmpty())

        kubriko.tickUntil { actorManager.allActors.value.any { it is ParticleBatch } }
        actorManager.awaitProcessed()
        assertEquals(1, actorManager.allActors.value.count { it is ParticleBatch })
    }

    private fun newParticleKubriko(cacheSize: Int = 1000) = newManualKubriko(
        ActorManager.newInstance(shouldComposeLayers = false),
        ParticleManager.newInstance(cacheSize = cacheSize),
    ).also { kubriko = it }

    private class TestEmitter(
        override var particleEmissionMode: ParticleEmitter.Mode,
        private val lifetimeInMilliseconds: Int = Int.MAX_VALUE,
    ) : ParticleEmitter<TestParticleState> {
        override val particleStateType = TestParticleState::class
        val createdStates = mutableListOf<TestParticleState>()
        var reusedCount = 0
            private set
        val emittedCount get() = createdStates.size + reusedCount

        override fun createParticleState() = TestParticleState(lifetimeInMilliseconds).also { createdStates.add(it) }

        override fun reuseParticleState(state: TestParticleState) {
            reusedCount++
            state.reset(lifetimeInMilliseconds)
        }
    }

    private class TestParticleState(private var remainingLifetimeInMilliseconds: Int) : ParticleEmitter.ParticleState() {
        override val body = BoxBody()
        val ageDeltas = mutableListOf<Int>()

        fun reset(lifetimeInMilliseconds: Int) {
            remainingLifetimeInMilliseconds = lifetimeInMilliseconds
            ageDeltas.clear()
        }

        override fun update(deltaTimeInMilliseconds: Int): Boolean {
            ageDeltas.add(deltaTimeInMilliseconds)
            remainingLifetimeInMilliseconds -= deltaTimeInMilliseconds
            return remainingLifetimeInMilliseconds > 0
        }

        override fun DrawScope.draw() = Unit
    }
}
