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

import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Contains details about a collision between two [Collidable] objects.
 *
 * @property contact The point in scene units where the collision occurred.
 * @property contactNormal The direction of the collision from the first object towards the second.
 * @property penetration The depth of the overlap between the two objects.
 */
class CollisionResult internal constructor(
    val contact: SceneOffset,
    val contactNormal: SceneOffset,
    val penetration: SceneUnit,
)