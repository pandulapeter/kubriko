/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles.implementation

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.particles.ParticleManagerImpl

internal class Particle<S : ParticleEmitter.ParticleState>(
    internal val state: S,
) : Visible, Dynamic {
    override val body get() = state.body
    override val drawingOrder get() = state.drawingOrder
    private lateinit var actorManager: ActorManager
    private lateinit var particleManager: ParticleManagerImpl

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        particleManager = kubriko.get()
    }

    override fun onRemoved() = particleManager.addParticleToCache(state::class, this)

    override fun update(deltaTimeInMilliseconds: Int) {
        if (!state.update(deltaTimeInMilliseconds)) {
            actorManager.remove(this)
        }
    }

    override fun DrawScope.draw() = with(state) { draw() }
}