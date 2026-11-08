/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import com.pandulapeter.kubriko.helpers.TriangleBatch

/**
 * A look that emits geometry of its own into a [TriangleBatch], for shapes the declarative looks don't cover (a game's
 * cuboids, a mesh, a stroked path).
 *
 * [emit] works in the body's own coordinates, like `Visible.draw`: (0, 0) is the body's top-left corner and
 * ([width], [height]) its bottom-right one, before its scale and rotation, which the engine applies to the emitted
 * vertices afterwards. Geometry should stay inside that rectangle, since nothing clips it. The engine calls [emit] only
 * when this look's [revision] changes ([invalidate]), when the body moves, turns, or resizes, or when the zoom does;
 * between those it draws what was emitted last. Every call starts from an empty batch.
 *
 * Batched actors whose custom looks share a [texture] (or have none) and its sampling are drawn together in one call,
 * along with any other looks drawn the same way.
 */
abstract class CustomLook : Look() {

    /**
     * The texture the emitted geometry samples through [TriangleBatch.addTexturedVertex] and
     * [TriangleBatch.addTexturedQuad], or null (the default) for untextured geometry. Vertices that sample nothing take
     * the batch's [TriangleBatch.defaultU] and [TriangleBatch.defaultV], which [emit] may set.
     */
    open val texture: ImageBitmap? get() = null

    /** How [texture] continues past its edges; see [TriangleBatch.textureTileMode]. */
    open val textureTileMode: TileMode get() = TileMode.Repeated

    /** How [texture] is filtered; see [TriangleBatch.textureFilterQuality]. */
    open val textureFilterQuality: FilterQuality get() = FilterQuality.Medium

    /**
     * Emits this look into [batch], in the body's own coordinates. [pixelsPerUnit] is how many screen pixels one of
     * those units covers, for geometry that depends on its size on screen, such as
     * [TriangleBatch.addLine]'s anti-aliased fringes. Runs on the drawing thread, on every frame for an actor that keeps
     * moving, so it should allocate nothing.
     */
    abstract fun emit(batch: TriangleBatch, width: Float, height: Float, pixelsPerUnit: Float)

    /** Marks the emitted geometry stale, so [emit] runs again before the next frame draws this look. */
    fun invalidate() = markChanged()
}
