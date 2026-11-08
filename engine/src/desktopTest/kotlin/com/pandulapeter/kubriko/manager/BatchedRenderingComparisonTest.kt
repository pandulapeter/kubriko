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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.looks.EllipseLook
import com.pandulapeter.kubriko.actor.looks.Look
import com.pandulapeter.kubriko.actor.looks.RectangleLook
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Batched
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the same scene through plain `Visible` actors and through batched ones, with real Skia, and compares the two
 * images. They may differ only along the shapes' edges, where the batch's fringe and Skia's anti-aliasing smooth a
 * little differently.
 */
class BatchedRenderingComparisonTest {

    private val imageSize = 160

    /** A 2 × 2 texture of four colors, so a sprite drawn wrong (flipped, offset, or wrapping) shows. */
    private val texture = ImageBitmap(2, 2).also { bitmap ->
        val canvas = Canvas(bitmap)
        listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow).forEachIndexed { index, color ->
            canvas.drawRect((index % 2).toFloat(), (index / 2).toFloat(), (index % 2) + 1f, (index / 2) + 1f, Paint().apply { this.color = color })
        }
    }

    private fun body(x: Float, y: Float, rotation: Float, scale: Scale = Scale.Unit) = BoxBody(
        initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit),
        initialSize = SceneSize(40f.sceneUnit, 30f.sceneUnit),
        initialScale = scale,
        initialRotation = rotation.rad,
    )

    private class PlainActor(override val body: BoxBody, private val drawing: DrawScope.(Size) -> Unit) : Actor, Visible {
        override fun DrawScope.draw() = drawing(body.size.raw)
    }

    private class BatchedActor(override val body: BoxBody, override val look: Look) : Actor, Batched

    private fun scene(isBatched: Boolean): List<Visible> = listOf(
        if (isBatched) {
            BatchedActor(body(40f, 40f, 0.3f), RectangleLook(Color.Magenta))
        } else {
            PlainActor(body(40f, 40f, 0.3f)) { drawRect(Color.Magenta, size = it) }
        },
        if (isBatched) {
            BatchedActor(body(110f, 50f, -0.5f, Scale(1.4f, 0.8f)), EllipseLook(Color.Cyan))
        } else {
            PlainActor(body(110f, 50f, -0.5f, Scale(1.4f, 0.8f))) { drawOval(Color.Cyan, size = it) }
        },
        if (isBatched) {
            BatchedActor(body(80f, 115f, 0f, Scale(2f, 2f)), SpriteLook(texture, filterQuality = FilterQuality.None))
        } else {
            PlainActor(body(80f, 115f, 0f, Scale(2f, 2f))) {
                drawImage(texture, dstSize = IntSize(it.width.toInt(), it.height.toInt()), filterQuality = FilterQuality.None)
            }
        },
        if (isBatched) {
            BatchedActor(body(30f, 120f, 0f), SpriteLook(texture, tint = Color(0xFF8080FF), filterQuality = FilterQuality.None))
        } else {
            PlainActor(body(30f, 120f, 0f)) {
                drawImage(
                    texture,
                    dstSize = IntSize(it.width.toInt(), it.height.toInt()),
                    colorFilter = ColorFilter.tint(Color(0xFF8080FF), BlendMode.Modulate),
                    filterQuality = FilterQuality.None,
                )
            }
        },
    )

    private fun render(isBatched: Boolean, isDrawnDirectly: Boolean = false): IntArray {
        val bitmap = ImageBitmap(imageSize, imageSize)
        val target = CanvasLayerTarget()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(imageSize.toFloat(), imageSize.toFloat())) {
            drawRect(Color.White)
            target.drawScope = this
            val scene = scene(isBatched)
            if (isDrawnDirectly) {
                scene.forEach(target::drawPlain)
            } else {
                BatchedLayerDrawer().draw(scene, target, pixelsPerUnit = 1f, isTextureSampledPerVertex = true)
            }
            target.drawScope = null
        }
        return IntArray(imageSize * imageSize).also { bitmap.readPixels(it) }
    }

    private fun difference(first: Int, second: Int): Int {
        var maximum = 0
        for (shift in intArrayOf(0, 8, 16, 24)) {
            maximum = maxOf(maximum, abs(((first shr shift) and 0xFF) - ((second shr shift) and 0xFF)))
        }
        return maximum
    }

    @Test
    fun `batched actors draw like plain ones`() = assertSimilar(render(isBatched = false), render(isBatched = true))

    @Test
    fun `a batched actor drawn directly, as the scene editor does, draws its look`() =
        assertSimilar(render(isBatched = false), render(isBatched = true, isDrawnDirectly = true))

    private fun assertSimilar(plain: IntArray, batched: IntArray) {
        val coloredPixels = plain.count { it != Color.White.value.shr(32).toInt() }
        val mismatches = plain.indices.count { difference(plain[it], batched[it]) > EDGE_TOLERANCE }
        assertTrue(coloredPixels > 5_000, "The scene should cover a good part of the image, covers $coloredPixels pixels.")
        assertTrue(
            mismatches < coloredPixels / 50,
            "$mismatches of $coloredPixels drawn pixels differ by more than $EDGE_TOLERANCE between plain and batched drawing.",
        )
    }

    private companion object {
        /** How far an edge pixel may differ: the two smooth edges differently, but never by a whole color. */
        const val EDGE_TOLERANCE = 64
    }
}
