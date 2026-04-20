/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.mask

import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

/**
 * A collision mask that has a size and can check if a point is inside it.
 */
sealed interface ComplexCollisionMask : CollisionMask {

    /**
     * The size of the mask in scene units.
     */
    val size: SceneSize

    /**
     * Checks if the given [sceneOffset] is inside the mask.
     */
    fun isSceneOffsetInside(sceneOffset: SceneOffset): Boolean
}