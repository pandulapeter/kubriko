/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.particleStates.ExplosionParticleState
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.SceneOffset

internal class Explosion(
    private val position: SceneOffset,
    private val colors: List<Color>,
) : ParticleEmitter<ExplosionParticleState> {
    private lateinit var actorManager: ActorManager
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Burst(emissionsPerBurst = 100)
        set(value) {
            field = value
            actorManager.remove(this)
        }
    override val particleStateType = ExplosionParticleState::class

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        kubriko.get<ScoreManager>().incrementScore()
    }

    override fun createParticleState() = ExplosionParticleState(
        position = position,
        color = colors.random(),
    )

    override fun reuseParticleState(state: ExplosionParticleState) = state.reset(
        position = position,
        color = colors.random(),
    )
}