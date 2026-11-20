/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

/**
 * Which layers of a Scene the editor hides and locks: the Actors of a hidden layer aren't drawn, and those of a hidden or
 * a locked one can't be picked, selected by a box, or selected all. Neither changes the Scene; it is the editor's own.
 *
 * @param hidden The layers not drawn, by Kubriko's `LayerAware.layerIndex`. Actors without a layer can't be hidden,
 *   since Kubriko draws them outside every layer.
 * @param locked The layers whose Actors stay where they are, null standing for Actors without a layer.
 */
public data class SceneLayerFilter(
    val hidden: Set<Int> = emptySet(),
    val locked: Set<Int?> = emptySet(),
) {
    /**
     * Whether the Actors of [layerIndex] can be picked.
     */
    public fun isPickable(layerIndex: Int?): Boolean = layerIndex !in locked && layerIndex !in hidden

    /**
     * This filter with [layerIndex] shown or hidden.
     */
    public fun withHidden(layerIndex: Int, isHidden: Boolean): SceneLayerFilter = copy(hidden = if (isHidden) hidden + layerIndex else hidden - layerIndex)

    /**
     * This filter with [layerIndex] locked or not.
     */
    public fun withLocked(layerIndex: Int?, isLocked: Boolean): SceneLayerFilter = copy(locked = if (isLocked) locked + layerIndex else locked - layerIndex)
}
