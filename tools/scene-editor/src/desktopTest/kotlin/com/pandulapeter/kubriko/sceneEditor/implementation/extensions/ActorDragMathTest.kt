/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.extensions

import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorDragMathTest {

    @Test
    fun scaleFollowsTheMouseDeltaRelativeToTheSizeAndIsClamped() {
        val scale = draggedScale(
            startScale = Scale.Unit,
            mouseDelta = SceneOffset(50f.sceneUnit, (-200f).sceneUnit),
            size = SceneSize(100f.sceneUnit, 100f.sceneUnit),
        )
        assertEquals(1.5f, scale.horizontal, TOLERANCE)
        assertEquals(0.05f, scale.vertical, TOLERANCE)
    }

    @Test
    fun zeroSizeKeepsTheStartScaleOnThatAxis() {
        val scale = draggedScale(
            startScale = Scale(2f, 3f),
            mouseDelta = SceneOffset(50f.sceneUnit, 50f.sceneUnit),
            size = SceneSize(0f.sceneUnit, 100f.sceneUnit),
        )
        assertEquals(2f, scale.horizontal, TOLERANCE)
        assertEquals(3.5f, scale.vertical, TOLERANCE)
    }

    @Test
    fun rotationAddsThePointerAngleDelta() {
        val rotation = draggedRotation(
            startRotation = 1f.rad,
            startPointerAngle = 0.5f.rad,
            currentPointerAngle = 1.25f.rad,
        )
        assertEquals(1.75f, rotation.raw, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
