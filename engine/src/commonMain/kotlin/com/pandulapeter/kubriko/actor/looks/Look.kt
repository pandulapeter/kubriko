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

import com.pandulapeter.kubriko.actor.traits.Batched

/**
 * What a [Batched] actor looks like: a description the engine turns into triangles, rather than code that draws.
 *
 * The declarative looks ([RectangleLook], [EllipseLook], and [SpriteLook]) cover shapes and sprites; a [CustomLook]
 * emits geometry of its own. Each look fills its actor's body (its size, before the body's scale and rotation), so it
 * needs no clipping.
 *
 * A look remembers the geometry it was last turned into, and rebuilds it only when its [revision] or its actor's body
 * changes, so a look should belong to a single actor: one shared between several still draws correctly, but rebuilds
 * its geometry for each of them on every frame.
 */
sealed class Look {

    /**
     * Grows by one with every change to this look, so whatever caches something built from it (the engine's geometry
     * among others) can tell that it is stale by comparing a number.
     */
    var revision = 0
        private set

    internal val geometry = LookGeometry()

    internal fun markChanged() {
        revision++
    }
}
