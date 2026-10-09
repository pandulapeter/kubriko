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
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs against a 1920 × 1080 viewport unless a test resizes it.
 */
class ViewportManagerTest {

    private val cameraPositions = listOf(SceneOffset.Zero, offset(300f, -150f))
    private val scaleFactors = listOf(1f, 2f)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun box(left: Float, top: Float, right: Float, bottom: Float) = AxisAlignedBoundingBox(
        min = offset(left, top),
        max = offset(right, bottom),
    )

    private fun assertClose(expected: Float, actual: Float, message: String? = null) {
        val tolerance = max(1e-3f, abs(expected) * 1e-4f)
        assertTrue(abs(expected - actual) <= tolerance, "${message.orEmpty()} expected $expected, got $actual")
    }

    private fun assertClose(expected: SceneOffset, actual: SceneOffset, message: String? = null) {
        assertClose(expected.x.raw, actual.x.raw, "${message.orEmpty()} x")
        assertClose(expected.y.raw, actual.y.raw, "${message.orEmpty()} y")
    }

    private fun ViewportManager.visibleArea() = bottomRight.value - topLeft.value

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
    fun defaultsMatchTheDocumentation() = withViewport { viewportManager ->
        assertEquals(SceneOffset.Zero, viewportManager.cameraPosition.value)
        assertEquals(Scale.Unit, viewportManager.scaleFactor.value)
        assertEquals(0.2f, viewportManager.minimumScaleFactor)
        assertEquals(5f, viewportManager.maximumScaleFactor)
        assertEquals(TargetFrameRate.Limit(60), viewportManager.targetFrameRate.value)
    }

    @Test
    fun setScaleFactorIsClampedToItsBounds() = withViewport(
        ViewportManager.newInstance(minimumScaleFactor = 0.5f, maximumScaleFactor = 2f),
    ) { viewportManager ->
        viewportManager.setScaleFactor(10f)
        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)

        viewportManager.setScaleFactor(0.01f)
        assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun multiplyScaleFactorIsClampedToItsBounds() = withViewport(
        ViewportManager.newInstance(minimumScaleFactor = 0.5f, maximumScaleFactor = 2f),
    ) { viewportManager ->
        repeat(10) { viewportManager.multiplyScaleFactor(1.5f) }
        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)

        repeat(10) { viewportManager.multiplyScaleFactor(1 / 1.5f) }
        assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
    }

    @Test
    fun initialScaleFactorIsClampedToItsBounds() {
        val expectedScales = listOf(
            ViewportManager.newInstance(initialScaleFactor = 50f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f) to 2f,
            ViewportManager.newInstance(initialScaleFactor = 0.01f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f) to 0.5f,
            ViewportManager.newInstance(minimumScaleFactor = 2f, maximumScaleFactor = 4f) to 2f,
            ViewportManager.newInstance(initialScaleFactor = 1.5f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f) to 1.5f,
        )
        for ((viewportManager, expectedScale) in expectedScales) {
            withViewport(viewportManager) {
                assertEquals(Scale(expectedScale, expectedScale), it.rawScaleFactor.value)
            }
        }
    }

    @Test
    fun visibleAreaIsCenteredOnTheCameraAndShrinksWithZoom() = withViewport { viewportManager ->
        forEachCameraAndScale(viewportManager) { camera, scale ->
            assertClose(camera, (viewportManager.topLeft.value + viewportManager.bottomRight.value) / 2, "center at $camera, $scale")
            assertClose(1920f / scale, viewportManager.visibleArea().x.raw, "width at $camera, $scale")
            assertClose(1080f / scale, viewportManager.visibleArea().y.raw, "height at $camera, $scale")
        }
    }

    @Test
    fun collectedValuesSettleOnTheCurrentValue() = withViewport { viewportManager ->
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
    fun screenCornersAndCenterMapOntoTheVisibleArea() = withViewport { viewportManager ->
        forEachCameraAndScale(viewportManager) { camera, scale ->
            assertClose(viewportManager.topLeft.value, Offset.Zero.toSceneOffset(viewportManager), "top left at $camera, $scale")
            assertClose(viewportManager.bottomRight.value, Offset(1920f, 1080f).toSceneOffset(viewportManager), "bottom right at $camera, $scale")
            assertClose(camera, Offset(960f, 540f).toSceneOffset(viewportManager), "center at $camera, $scale")
        }
    }

    @Test
    fun screenDistancesSurviveARoundTripThroughTheScene() = withViewport { viewportManager ->
        val pixels = listOf(Offset.Zero, Offset(1920f, 0f), Offset(0f, 1080f), Offset(1920f, 1080f), Offset(960f, 540f))
        forEachCameraAndScale(viewportManager) { camera, scale ->
            for (p in pixels) {
                for (q in pixels) {
                    val delta = (p.toSceneOffset(viewportManager) - q.toSceneOffset(viewportManager)).toOffset(viewportManager)
                    assertClose(p.x - q.x, delta.x, "x delta at $camera, $scale")
                    assertClose(p.y - q.y, delta.y, "y delta at $camera, $scale")
                }
            }
        }
    }

    @Test
    fun sceneVectorToScreenIgnoresTheCamera() = withViewport { viewportManager ->
        val vector = offset(10f, -20f)
        viewportManager.setScaleFactor(2f)
        val before = vector.toOffset(viewportManager)

        viewportManager.setCameraPosition(offset(-999f, 999f))

        assertEquals(Offset(20f, -40f), before)
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
    fun sizeConversionDependsOnlyOnTheScale() = withViewport { viewportManager ->
        forEachCameraAndScale(viewportManager) { camera, scale ->
            assertEquals(
                SceneSize((100f / scale).sceneUnit, (50f / scale).sceneUnit),
                Size(100f, 50f).toSceneSize(viewportManager),
                "at $camera, $scale",
            )
        }
    }

    @Test
    fun aspectRatioModesFixTheVisibleSceneArea() {
        val expectedAreas = listOf(
            ViewportManager.AspectRatioMode.Dynamic to offset(800f, 600f),
            ViewportManager.AspectRatioMode.FitHorizontal(400f.sceneUnit) to offset(400f, 300f),
            ViewportManager.AspectRatioMode.FitVertical(300f.sceneUnit) to offset(400f, 300f),
            ViewportManager.AspectRatioMode.Fixed(ratio = 4f / 3f, width = 400f.sceneUnit) to offset(400f, 300f),
            ViewportManager.AspectRatioMode.Stretched(SceneSize(400f.sceneUnit, 300f.sceneUnit)) to offset(400f, 300f),
            ViewportManager.AspectRatioMode.Stretched(SceneSize(400f.sceneUnit, 200f.sceneUnit)) to offset(400f, 200f),
        )
        for ((aspectRatioMode, expectedArea) in expectedAreas) {
            withViewport(ViewportManager.newInstance(aspectRatioMode = aspectRatioMode)) { viewportManager ->
                viewportManager.onViewportSizeChanged(800f, 600f)

                assertEquals(Size(800f, 600f), viewportManager.size.value, "size for $aspectRatioMode")
                assertClose(expectedArea, viewportManager.visibleArea(), "visible area for $aspectRatioMode")
            }
        }
    }

    @Test
    fun aspectRatioModeKeepsItsVisibleAreaWhenTheScreenResizes() = withViewport(
        ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.FitHorizontal(400f.sceneUnit)),
    ) { viewportManager ->
        viewportManager.onViewportSizeChanged(800f, 600f)
        viewportManager.onViewportSizeChanged(1600f, 900f)

        assertClose(offset(400f, 225f), viewportManager.visibleArea())
    }

    @Test
    fun zoomComposesWithTheAspectRatioMode() = withViewport(
        ViewportManager.newInstance(aspectRatioMode = ViewportManager.AspectRatioMode.FitHorizontal(400f.sceneUnit)),
    ) { viewportManager ->
        viewportManager.onViewportSizeChanged(800f, 600f)

        viewportManager.setScaleFactor(2f)

        assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        assertEquals(Scale(4f, 4f), viewportManager.scaleFactor.value)
        assertClose(offset(200f, 150f), viewportManager.visibleArea())
    }

    @Test
    fun targetFrameRateCanBeChangedAtRuntime() = withViewport(
        ViewportManager.newInstance(initialTargetFrameRate = TargetFrameRate.DisplayDivider(2)),
    ) { viewportManager ->
        assertEquals(TargetFrameRate.DisplayDivider(2), viewportManager.targetFrameRate.value)

        viewportManager.setTargetFrameRate(TargetFrameRate.Limit(30))

        assertEquals(TargetFrameRate.Limit(30), viewportManager.targetFrameRate.value)
    }

    @Test
    fun boundsTouchingTheViewportCountAsWithinIt() = withViewport { viewportManager ->
        assertTrue(box(-5f, -5f, 5f, 5f).isWithinViewportBounds(viewportManager))
        assertTrue(box(960f, 0f, 970f, 10f).isWithinViewportBounds(viewportManager))
        assertTrue(box(0f, -550f, 10f, -540f).isWithinViewportBounds(viewportManager))
        assertFalse(box(960.5f, 0f, 970f, 10f).isWithinViewportBounds(viewportManager))
        assertFalse(box(0f, 540.5f, 10f, 550f).isWithinViewportBounds(viewportManager))
    }

    @Test
    fun viewportBoundsFollowTheCameraAndZoom() = withViewport { viewportManager ->
        val bounds = box(1000f, 0f, 1010f, 10f)
        assertFalse(bounds.isWithinViewportBounds(viewportManager))

        viewportManager.setCameraPosition(offset(100f, 0f))
        assertTrue(bounds.isWithinViewportBounds(viewportManager))

        viewportManager.setScaleFactor(2f)
        assertFalse(bounds.isWithinViewportBounds(viewportManager))
    }

    @Test
    fun edgeBufferWidensTheViewportBounds() = withViewport(
        ViewportManager.newInstance(viewportEdgeBuffer = 50f.sceneUnit),
    ) { viewportManager ->
        assertTrue(box(1010f, 0f, 1020f, 10f).isWithinViewportBounds(viewportManager))
        assertFalse(box(1010.5f, 0f, 1020f, 10f).isWithinViewportBounds(viewportManager))
    }
}
