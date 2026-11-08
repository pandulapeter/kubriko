/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.Scale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderingModeScaleTest {

    private val kubriko = newManualKubriko()

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun `a box switched to the other kind keeps its scale when added again`() {
        val plain = MovingBox(MovingBox.State())
        plain.body.scale = Scale(1.3f, 1.3f)

        val batched = plain.inRenderingMode(isBatched = true)
        batched.onAdded(kubriko.kubriko)

        assertEquals(Scale(1.3f, 1.3f), (batched as BatchedMovingBox).body.scale)
    }

    @Test
    fun `a freshly loaded box still starts from a random scale`() {
        val scales = List(20) {
            MovingBox(MovingBox.State()).apply { onAdded(kubriko.kubriko) }.body.scale.horizontal
        }

        assertEquals(true, scales.all { it in 0.5f..1.6f } && scales.toSet().size > 1)
    }
}
