/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.ComplexBody
import kotlin.reflect.KClass

interface ParticleEmitter<S : ParticleEmitter.ParticleState> : Actor {

    var particleEmissionMode: Mode
    val particleStateType: KClass<S>

    fun createParticleState(): S

    fun reuseParticleState(state: S)

    @Suppress("UNCHECKED_CAST")
    fun reuseParticleInternal(state: ParticleState) = reuseParticleState(state as S)

    sealed class Mode {

        data class Continuous(val emissionsPerMillisecond: Float) : Mode()

        data class Burst(val emissionsPerBurst: Int) : Mode()

        data object Inactive : Mode()
    }

    abstract class ParticleState {
        abstract val body: ComplexBody
        open val drawingOrder: Float = 0f

        abstract fun update(deltaTimeInMilliseconds: Int): Boolean

        abstract fun DrawScope.draw()
    }
}