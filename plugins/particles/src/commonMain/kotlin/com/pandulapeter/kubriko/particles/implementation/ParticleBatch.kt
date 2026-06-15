/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.particles.implementation

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.ParticleEmitter
import kotlin.concurrent.Volatile

/**
 * Draws every live particle that shares one [drawingOrder] in a single actor, so spawning and
 * recycling no longer add and remove an actor per particle. One batch exists per distinct
 * [drawingOrder] to preserve interleaving with the rest of the scene.
 */
internal class ParticleBatch(
    override val drawingOrder: Float,
) : Visible {

    override val body = BoxBody()
    override val shouldClip = false
    override val isAlwaysVisible = true

    /**
     * Double buffer so the render thread never iterates a list the tick thread is mutating: the
     * working buffer is filled off [renderList], then published with a single volatile swap.
     */
    @Volatile
    private var renderList: List<ParticleEmitter.ParticleState> = emptyList()
    private val bufferA = ArrayList<ParticleEmitter.ParticleState>()
    private val bufferB = ArrayList<ParticleEmitter.ParticleState>()
    private var workingIsA = true

    private var viewportManager: ViewportManager? = null

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    fun beginFrame(
        deltaTimeInMilliseconds: Int,
        isRunning: Boolean,
        recycle: (ParticleEmitter.ParticleState) -> Unit,
    ) {
        val working = if (workingIsA) bufferA else bufferB
        working.clear()
        val source = renderList
        for (i in source.indices) {
            val state = source[i]
            if (!isRunning || state.update(deltaTimeInMilliseconds)) {
                working.add(state)
            } else {
                recycle(state)
            }
        }
    }

    fun addParticle(state: ParticleEmitter.ParticleState) {
        (if (workingIsA) bufferA else bufferB).add(state)
    }

    fun endFrame() {
        renderList = if (workingIsA) bufferA else bufferB
        workingIsA = !workingIsA
    }

    override fun DrawScope.draw() {
        val viewportManager = viewportManager ?: return
        val particles = renderList
        if (particles.isEmpty()) return
        val topLeft = viewportManager.topLeft.value
        val bottomRight = viewportManager.bottomRight.value
        val leftBound = topLeft.x.raw
        val topBound = topLeft.y.raw
        val rightBound = bottomRight.x.raw
        val bottomBound = bottomRight.y.raw
        for (i in particles.indices) {
            val state = particles[i]
            val aabb = state.body.axisAlignedBoundingBox
            if (aabb.left.raw <= rightBound &&
                aabb.top.raw <= bottomBound &&
                aabb.right.raw >= leftBound &&
                aabb.bottom.raw >= topBound
            ) {
                withTransform(
                    transformBlock = { state.body.transformForViewport(this) },
                    drawBlock = {
                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = state.body.size.width.raw,
                            bottom = state.body.size.height.raw,
                        ) {
                            with(state) { draw() }
                        }
                    },
                )
            }
        }
    }
}
