/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.mask

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

class BoxCollisionMask(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PolygonCollisionMask(
    unprocessedVertices = listOf(
        SceneOffset.Zero - initialSize.center,
        SceneOffset(initialSize.width, SceneUnit.Zero) - initialSize.center,
        SceneOffset(initialSize.width, initialSize.height) - initialSize.center,
        SceneOffset(SceneUnit.Zero, initialSize.height) - initialSize.center,
    ),
    initialPosition = initialPosition,
    initialRotation = initialRotation,
)