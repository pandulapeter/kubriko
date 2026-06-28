/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Describes where a ray first enters a [CollisionMask].
 *
 * @property mask The mask that was hit.
 * @property point The point in scene units where the ray enters the mask.
 * @property normal The unit surface normal at [point], pointing outward from the mask (back toward the
 * ray's origin).
 * @property distance The distance from the ray's origin to [point].
 */
class RaycastHit internal constructor(
    val mask: CollisionMask,
    val point: SceneOffset,
    val normal: SceneOffset,
    val distance: SceneUnit,
)
