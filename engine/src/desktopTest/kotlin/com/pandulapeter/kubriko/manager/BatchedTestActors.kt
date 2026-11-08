/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.looks.CustomLook
import com.pandulapeter.kubriko.actor.looks.Look
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

internal class TestBatched(
    override var look: Look,
    override var drawingOrder: Float = 0f,
    size: Float = 10f,
) : Actor, Batched {
    override val body = BoxBody(initialSize = SceneSize(size.sceneUnit, size.sceneUnit))
    override var isVisible = true
}

internal class TestPlain(val name: String, override val drawingOrder: Float = 0f) : Actor, Visible {
    override val body = BoxBody(initialPosition = SceneOffset.Zero, initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))

    override fun DrawScope.draw() = Unit
}

/** Emits [quadCount] quads, four vertices each, and counts how often it was asked to. */
internal class QuadsLook(private val quadCount: Int) : CustomLook() {
    var emitCount = 0

    override fun emit(batch: TriangleBatch, width: Float, height: Float, pixelsPerUnit: Float) {
        emitCount++
        repeat(quadCount) { batch.addQuad(0f, 0f, width, 0f, width, height, 0f, height, argb = -1) }
    }
}

/** Records what the drawer draws, as `plain:<name>`, `run:<vertices>`, and `sprite`. */
internal class RecordingLayerTarget : BatchedLayerTarget {
    val events = mutableListOf<String>()
    val runBatches = mutableListOf<TriangleBatch>()

    override fun drawPlain(visible: Visible) {
        events += "plain:${(visible as TestPlain).name}"
    }

    override fun drawRun(batch: TriangleBatch) {
        events += "run:${batch.vertexCount}"
        runBatches += batch
    }

    override fun drawSpriteIndividually(actor: Batched, look: SpriteLook) {
        events += "sprite"
    }
}

/** Counts nothing and keeps nothing, for measuring allocations. */
internal object SilentLayerTarget : BatchedLayerTarget {
    var runCount = 0

    override fun drawPlain(visible: Visible) = Unit

    override fun drawRun(batch: TriangleBatch) {
        runCount++
    }

    override fun drawSpriteIndividually(actor: Batched, look: SpriteLook) = Unit
}
