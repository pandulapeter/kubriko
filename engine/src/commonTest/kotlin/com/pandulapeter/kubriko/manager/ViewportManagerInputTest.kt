/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The documented handling of non-finite camera and zoom input.
 */
class ViewportManagerInputTest {

    private val tickSource = TickSource.manual()
    private val kubriko = Kubriko.newInstance(
        ViewportManager.newInstance(minimumScaleFactor = 0.5f, maximumScaleFactor = 4f),
        tickSource = tickSource,
    ).also { tickSource.start() }
    private val viewportManager = kubriko.get<ViewportManager>()

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun nanScaleFactorIsIgnored() {
        viewportManager.setScaleFactor(2f)

        viewportManager.setScaleFactor(Float.NaN)

        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun multiplicationWithANanResultIsIgnored() {
        viewportManager.setScaleFactor(2f)

        viewportManager.multiplyScaleFactor(Float.NaN)

        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun infiniteScaleFactorsAreClamped() {
        viewportManager.setScaleFactor(Float.POSITIVE_INFINITY)
        assertEquals(Scale(4f, 4f), viewportManager.rawScaleFactor.value)

        viewportManager.setScaleFactor(Float.NEGATIVE_INFINITY)
        assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)

        viewportManager.multiplyScaleFactor(Float.POSITIVE_INFINITY)
        assertEquals(Scale(4f, 4f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun multiplyingByZeroClampsToTheMinimum() {
        viewportManager.multiplyScaleFactor(0f)

        assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun nonFiniteCameraPositionIsIgnored() {
        val position = SceneOffset(10f.sceneUnit, 20f.sceneUnit)
        viewportManager.setCameraPosition(position)

        viewportManager.setCameraPosition(SceneOffset(Float.NaN.sceneUnit, 0f.sceneUnit))
        viewportManager.setCameraPosition(SceneOffset(0f.sceneUnit, Float.POSITIVE_INFINITY.sceneUnit))

        assertEquals(position, viewportManager.cameraPosition.value)
    }

    @Test
    fun nonFiniteCameraOffsetIsIgnored() {
        val position = SceneOffset(10f.sceneUnit, 20f.sceneUnit)
        viewportManager.setCameraPosition(position)

        viewportManager.addToCameraPosition(Offset(Float.NaN, 0f))
        viewportManager.addToCameraPosition(Offset(0f, Float.NEGATIVE_INFINITY))

        assertEquals(position, viewportManager.cameraPosition.value)
    }
}
