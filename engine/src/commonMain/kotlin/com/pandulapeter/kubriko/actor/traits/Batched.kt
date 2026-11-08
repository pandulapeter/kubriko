/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.traits

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.actor.looks.CustomLook
import com.pandulapeter.kubriko.actor.looks.Look
import com.pandulapeter.kubriko.actor.looks.drawLook

/**
 * A [Visible] that describes its [look] instead of drawing it, so that the engine can draw many such actors together.
 *
 * A plain [Visible] costs its own canvas operations every frame (a save, a transform, a clip, its [draw], a restore),
 * so a scene's drawing cost grows with its actor count. Within a layer, consecutive batched actors in drawing order are
 * drawn instead as runs of triangles, one native call per run: each actor's geometry is mapped by its body (position,
 * rotation, scale, and pivot) on the CPU, rebuilt only when the body or the look changes, and a run whose members
 * neither changed nor moved in the order draws last frame's arrays again without refilling them. A plain [Visible]
 * between batched ones ends the run before it and draws as it always has, so the paint order is exactly what
 * [drawingOrder] says. Batched actors of the same [drawingOrder], which the order leaves unordered, are grouped by
 * texture into longer runs.
 *
 * The declarative looks cover rectangles, ellipses, and sprites; a [CustomLook] emits geometry of its own. A look lies
 * inside the body by construction, so nothing is clipped and [shouldClip] is false. [draw] draws the look on its own,
 * for anything that draws a [Visible] directly (the scene editor and the debug tools among others); the engine's layers
 * never call it. Overriding it changes nothing about how the layers draw the actor.
 */
interface Batched : Visible {

    /**
     * What this actor looks like. Give every actor a look of its own: one shared between actors still draws correctly,
     * but rebuilds its geometry for each of them on every frame. Replacing the look with another one is fine at any
     * time; mutating the current one rebuilds only what it changed.
     */
    val look: Look

    /** A look never reaches past the body, so batched actors are never clipped. */
    override val shouldClip: Boolean get() = false

    /** Draws [look] into this [DrawScope], which is expected to be transformed to the body already. */
    override fun DrawScope.draw() = drawLook(look, body.size.width.raw, body.size.height.raw)
}
