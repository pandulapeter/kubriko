/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.transformForViewport
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import java.lang.reflect.Proxy
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks that the CPU transform of batched geometry puts every corner exactly where the canvas transform of a plain
 * `Visible` would. The canvas is a recording proxy, so no Skia is needed.
 */
class LookTransformTest {

    /** The matrix the canvas path applies to [body]: `transformForViewport` on a canvas that only records. */
    private fun canvasMatrixOf(body: BoxBody): Matrix {
        val matrix = Matrix()
        val canvas = Proxy.newProxyInstance(Canvas::class.java.classLoader, arrayOf(Canvas::class.java)) { _, method, arguments ->
            when (method.name) {
                "translate" -> matrix.translate(arguments[0] as Float, arguments[1] as Float)
                "rotate" -> matrix.rotateZ(arguments[0] as Float)
                "scale" -> matrix.scale(arguments[0] as Float, arguments[1] as Float)
                "save", "restore" -> Unit
                else -> error("Unexpected canvas call: ${method.name}")
            }
            null
        } as Canvas
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(100f, 100f)) {
            body.transformForViewport(drawContext.transform)
        }
        return matrix
    }

    private val bodies = listOf(
        BoxBody(initialPosition = SceneOffset(10f.sceneUnit, 20f.sceneUnit), initialSize = SceneSize(30f.sceneUnit, 40f.sceneUnit)),
        BoxBody(
            initialPosition = SceneOffset((-5f).sceneUnit, 7f.sceneUnit),
            initialSize = SceneSize(30f.sceneUnit, 40f.sceneUnit),
            initialRotation = 0.7f.rad,
        ),
        BoxBody(
            initialPosition = SceneOffset(100f.sceneUnit, 50f.sceneUnit),
            initialSize = SceneSize(64f.sceneUnit, 16f.sceneUnit),
            initialPivot = SceneOffset(4f.sceneUnit, 12f.sceneUnit),
            initialScale = Scale(1.5f, 0.5f),
            initialRotation = (-2.2f).rad,
        ),
        BoxBody(
            initialPosition = SceneOffset(3f.sceneUnit, 3f.sceneUnit),
            initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit),
            initialPivot = SceneOffset.Zero,
            initialScale = Scale(-2f, 1f),
            initialRotation = 4f.rad,
        ),
    )

    private fun assertNear(expected: Float, actual: Float, message: String) =
        assertTrue(abs(expected - actual) < 1e-3f, "$message: expected $expected, was $actual.")

    @Test
    fun `the cpu transform matches the canvas transform`() {
        val transform = LookTransform()
        for (body in bodies) {
            val matrix = canvasMatrixOf(body)
            transform.setToBody(body)
            for ((x, y) in listOf(0f to 0f, body.size.width.raw to 0f, body.size.width.raw to body.size.height.raw, 7f to 3f)) {
                val expected = matrix.map(Offset(x, y))
                assertNear(expected.x, transform.x(x, y), "x of ($x, $y) for $body")
                assertNear(expected.y, transform.y(x, y), "y of ($x, $y) for $body")
            }
        }
    }

    @Test
    fun `a rectangle's corners land where the canvas would draw them`() {
        val transform = LookTransform()
        for (body in bodies) {
            val matrix = canvasMatrixOf(body)
            transform.setToBody(body)
            val batch = TriangleBatch()
            // So many pixels per unit that the fringe is negligible and the inner corners are the corners.
            LookEmitter.emit(RectangleLook(Color.Red), batch, body.size.width.raw, body.size.height.raw, transform, 1e6f, isStandIn = false)

            assertEquals(8, batch.vertexCount)
            val width = body.size.width.raw
            val height = body.size.height.raw
            listOf(0f to 0f, width to 0f, width to height, 0f to height).forEachIndexed { corner, (x, y) ->
                val expected = matrix.map(Offset(x, y))
                assertNear(expected.x, batch.positions[corner * 4], "corner $corner x for $body")
                assertNear(expected.y, batch.positions[corner * 4 + 1], "corner $corner y for $body")
            }
        }
    }
}
