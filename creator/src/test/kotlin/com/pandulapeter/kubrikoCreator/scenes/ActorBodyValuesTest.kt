/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class ActorBodyValuesTest {

    @Test
    fun aBoxBodyShowsItsSizeRotationAndScale() {
        val body = BoxBody(
            initialPosition = SceneOffset(3f.sceneUnit, 4f.sceneUnit),
            initialSize = SceneSize(10f.sceneUnit, 20f.sceneUnit),
            initialScale = Scale(2f, 0.5f),
            initialRotation = (PI / 2).toFloat().rad,
        )
        val values = ActorBodyValues.of(body)
        assertEquals(ActorBodyValues(3f, 4f, 10f to 20f, values.rotationInDegrees, 2f, 0.5f), values)
        assertEquals(90f, values.rotationInDegrees)
        assertEquals(30f, ActorBodyValues.degreesOf((30 * PI / 180).toFloat()))
    }

    @Test
    fun aPointBodyHasOnlyAPosition() {
        assertEquals(ActorBodyValues(1f, 2f), ActorBodyValues.of(PointBody(initialPosition = SceneOffset(1f.sceneUnit, 2f.sceneUnit))))
    }
}
