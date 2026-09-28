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

class ViewportManagerInputTest {

    private val tickSource = TickSource.manual()
    private val kubriko = Kubriko.newInstance(tickSource = tickSource).also { tickSource.start() }
    private val viewportManager = kubriko.get<ViewportManager>()

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun nanScaleFactorIsIgnored() {
        viewportManager.setScaleFactor(2f)
        viewportManager.setScaleFactor(Float.NaN)
        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        viewportManager.multiplyScaleFactor(Float.NaN)
        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun nonFiniteCameraPositionIsIgnored() {
        val position = SceneOffset(10f.sceneUnit, 20f.sceneUnit)
        viewportManager.setCameraPosition(position)
        viewportManager.setCameraPosition(SceneOffset(Float.NaN.sceneUnit, 0f.sceneUnit))
        assertEquals(position, viewportManager.cameraPosition.value)
        viewportManager.setCameraPosition(SceneOffset(Float.POSITIVE_INFINITY.sceneUnit, 0f.sceneUnit))
        assertEquals(position, viewportManager.cameraPosition.value)
        viewportManager.addToCameraPosition(Offset(Float.NaN, 0f))
        assertEquals(position, viewportManager.cameraPosition.value)
    }

    @Test
    fun validValuesStillApply() {
        viewportManager.setScaleFactor(100f)
        assertEquals(Scale(viewportManager.maximumScaleFactor, viewportManager.maximumScaleFactor), viewportManager.rawScaleFactor.value)
    }
}
