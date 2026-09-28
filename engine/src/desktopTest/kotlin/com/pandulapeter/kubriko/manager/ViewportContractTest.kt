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
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toOffset
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.helpers.extensions.toSceneSize
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewportContractTest {

    private val cameraPositions = listOf(SceneOffset.Zero, offset(300f, -150f))
    private val scaleFactors = listOf(1f, 2f)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun assertClose(expected: Float, actual: Float, message: String? = null) {
        val tolerance = max(1e-3f, abs(expected) * 1e-4f)
        assertTrue(abs(expected - actual) <= tolerance, "${message.orEmpty()} expected $expected, got $actual")
    }

    private fun assertClose(expected: SceneOffset, actual: SceneOffset, message: String? = null) {
        assertClose(expected.x.raw, actual.x.raw, "${message.orEmpty()} x")
        assertClose(expected.y.raw, actual.y.raw, "${message.orEmpty()} y")
    }

    private fun withViewport(
        viewportManager: ViewportManager = ViewportManager.newInstance(),
        block: (ViewportManagerImpl) -> Unit,
    ) {
        val (kubriko, _) = newTestKubriko(viewportManager)
        try {
            block(kubriko.viewportManager)
        } finally {
            kubriko.dispose()
        }
    }

    private fun forEachCameraAndScale(viewportManager: ViewportManager, block: (SceneOffset, Float) -> Unit) {
        for (camera in cameraPositions) {
            for (scale in scaleFactors) {
                viewportManager.setCameraPosition(camera)
                viewportManager.setScaleFactor(scale)
                block(camera, scale)
            }
        }
    }

    @Test
    fun scaleFactorIsClampedToItsBounds() = withViewport(
        ViewportManager.newInstance(minimumScaleFactor = 0.5f, maximumScaleFactor = 2f),
    ) { viewportManager ->
        viewportManager.setScaleFactor(10f)
        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        viewportManager.setScaleFactor(0.01f)
        assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
        repeat(10) {
            viewportManager.multiplyScaleFactor(1.5f)
            assertTrue(viewportManager.rawScaleFactor.value.horizontal <= 2f)
        }
        repeat(10) {
            viewportManager.multiplyScaleFactor(1 / 1.5f)
            assertTrue(viewportManager.rawScaleFactor.value.horizontal >= 0.5f)
        }
    }

    @Test
    fun initialScaleFactorIsClampedToItsBounds() {
        withViewport(ViewportManager.newInstance(initialScaleFactor = 50f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(initialScaleFactor = 0.01f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(minimumScaleFactor = 2f, maximumScaleFactor = 4f)) { viewportManager ->
            assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(initialScaleFactor = 1.5f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(1.5f, 1.5f), viewportManager.rawScaleFactor.value)
        }
    }

    @Test
    fun visibleAreaMatchesTheCamera() = withViewport { viewportManager ->
        forEachCameraAndScale(viewportManager) { camera, scale ->
            val topLeft = viewportManager.topLeft.value
            val bottomRight = viewportManager.bottomRight.value
            assertClose(camera, (topLeft + bottomRight) / 2, "center at $camera, $scale")
            assertClose(1920f / scale, (bottomRight - topLeft).x.raw, "width at $camera, $scale")
            assertClose(1080f / scale, (bottomRight - topLeft).y.raw, "height at $camera, $scale")
        }
    }

    @Test
    fun syncGetterAgreesWithTheCollectedFlow() = withViewport { viewportManager ->
        fun <T> assertCollected(flow: StateFlow<T>) {
            val expected = flow.value
            runBlocking {
                withTimeout(2_000) {
                    assertEquals(expected, flow.first { it == expected })
                }
            }
        }
        val mutations = listOf<() -> Unit>(
            { viewportManager.setCameraPosition(offset(120f, -40f)) },
            { viewportManager.setScaleFactor(1.5f) },
            { viewportManager.updateSize(Size(800f, 600f)) },
            { viewportManager.addToCameraPosition(Offset(33f, 11f)) },
        )
        for (mutation in mutations) {
            mutation()
            assertCollected(viewportManager.topLeft)
            assertCollected(viewportManager.bottomRight)
            assertCollected(viewportManager.scaleFactor)
        }
    }

    @Test
    fun screenToSceneMapsTheVisibleArea() = withViewport { viewportManager ->
        val pixels = listOf(Offset.Zero, Offset(1920f, 0f), Offset(0f, 1080f), Offset(1920f, 1080f), Offset(960f, 540f))
        forEachCameraAndScale(viewportManager) { camera, scale ->
            assertClose(viewportManager.topLeft.value, Offset.Zero.toSceneOffset(viewportManager), "top left at $camera, $scale")
            assertClose(viewportManager.bottomRight.value, Offset(1920f, 1080f).toSceneOffset(viewportManager), "bottom right at $camera, $scale")
            assertClose(viewportManager.cameraPosition.value, Offset(960f, 540f).toSceneOffset(viewportManager), "center at $camera, $scale")
            for (p in pixels) {
                for (q in pixels) {
                    val delta = (p.toSceneOffset(viewportManager) - q.toSceneOffset(viewportManager)).toOffset(viewportManager)
                    assertClose(p.x - q.x, delta.x, "x delta at $camera, $scale")
                    assertClose(p.y - q.y, delta.y, "y delta at $camera, $scale")
                }
            }
        }
        val vector = offset(10f, -20f)
        val before = vector.toOffset(viewportManager)
        viewportManager.setCameraPosition(offset(-999f, 999f))
        assertEquals(before, vector.toOffset(viewportManager))
    }

    @Test
    fun addToCameraPositionMovesByScreenPixels() = withViewport { viewportManager ->
        for (scale in scaleFactors) {
            viewportManager.setScaleFactor(scale)
            viewportManager.setCameraPosition(offset(10f, 20f))
            viewportManager.addToCameraPosition(Offset(100f, -50f))
            assertClose(offset(10f + 100f / scale, 20f - 50f / scale), viewportManager.cameraPosition.value, "at $scale")
        }
    }

    @Test
    fun sizeConversionIsPositionIndependent() = withViewport { viewportManager ->
        forEachCameraAndScale(viewportManager) { camera, scale ->
            val converted = Size(100f, 50f).toSceneSize(viewportManager)
            assertClose(100f / scale, converted.width.raw, "width at $camera, $scale")
            assertClose(50f / scale, converted.height.raw, "height at $camera, $scale")
            assertEquals(SceneSize((100f / scale).sceneUnit, (50f / scale).sceneUnit), converted)
        }
    }

    @Test
    fun targetFrameRateRoundTrips() = withViewport(
        ViewportManager.newInstance(initialTargetFrameRate = TargetFrameRate.DisplayDivider(2)),
    ) { viewportManager ->
        assertEquals(TargetFrameRate.DisplayDivider(2), viewportManager.targetFrameRate.value)
        viewportManager.setTargetFrameRate(TargetFrameRate.Limit(30))
        assertEquals(TargetFrameRate.Limit(30), viewportManager.targetFrameRate.value)
    }
}
