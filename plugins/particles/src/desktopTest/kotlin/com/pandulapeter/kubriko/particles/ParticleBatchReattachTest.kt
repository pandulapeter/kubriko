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
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.particles.implementation.ParticleBatch
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticleBatchReattachTest {

    @Test
    fun batchIsReAddedExactlyOnceAfterRemoveAll() {
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            ParticleManager.newInstance(cacheSize = 10),
        )
        try {
            val actorManager = kubriko.actorManager
            actorManager.add(TestEmitter())
            kubriko.tickUntil { actorManager.allActors.value.any { it is ParticleBatch } }

            actorManager.removeAll()
            actorManager.awaitProcessed()
            assertTrue(actorManager.allActors.value.isEmpty())

            kubriko.tickUntil { actorManager.allActors.value.any { it is ParticleBatch } }
            actorManager.awaitProcessed()
            assertEquals(1, actorManager.allActors.value.count { it is ParticleBatch })
        } finally {
            kubriko.dispose()
        }
    }

    private class TestEmitter : ParticleEmitter<TestParticleState> {
        override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous { 1f }
        override val particleStateType = TestParticleState::class

        override fun createParticleState() = TestParticleState()

        override fun reuseParticleState(state: TestParticleState) = Unit
    }

    private class TestParticleState : ParticleEmitter.ParticleState() {
        override val body = BoxBody()

        override fun update(deltaTimeInMilliseconds: Int) = true

        override fun DrawScope.draw() = Unit
    }
}
