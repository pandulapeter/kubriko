/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.base

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.particleStates.BulletParticleState
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.helpers.extensions.times
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

internal abstract class Bullet(
    initialPosition: SceneOffset,
    private val direction: AngleRadians,
    private val playSoundEffect: AudioManager.() -> Unit,
    private val bulletColor: Color,
    private val bulletBaseSpeed: SceneUnit,
    private val speedIncrement: (Int) -> Float,
) : Visible, Dynamic, ParticleEmitter<BulletParticleState>, CollisionDetector {
    override val body = BoxBody(
        initialPosition = initialPosition,
        initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit),
    )
    private val radius = body.size.width / 2f
    protected lateinit var actorManager: ActorManager
    protected lateinit var audioManager: AudioManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var scoreManager: ScoreManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager
    override val drawingOrder = 1f
    override val particleStateType = BulletParticleState::class
    override var particleEmissionMode: ParticleEmitter.Mode = ParticleEmitter.Mode.Continuous(
        getEmissionsPerMillisecond = { 0.15f }
    )

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        gameplayManager = kubriko.get()
        scoreManager = kubriko.get()
        stateManager = kubriko.get()
        viewportManager = kubriko.get()
        audioManager.playSoundEffect()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value) {
            body.position += Offset(
                x = direction.cos,
                y = direction.sin,
            ) * bulletBaseSpeed * deltaTimeInMilliseconds * speedIncrement(scoreManager.score.value)
            if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
                actorManager.remove(this)
            }
        }
    }

    override fun createParticleState() = BulletParticleState(
        position = body.position,
        color = bulletColor,
    )

    override fun reuseParticleState(state: BulletParticleState) = state.reset(
        position = body.position,
        color = bulletColor,
    )

    override fun DrawScope.draw() = drawCircle(
        radius = radius.raw,
        center = body.size.center.raw,
        color = bulletColor,
    )
}