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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.actor.looks.EllipseLook
import com.pandulapeter.kubriko.actor.looks.RectangleLook
import com.pandulapeter.kubriko.actor.looks.SpriteLook
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BatchedLayerDrawerTest {

    private val drawer = BatchedLayerDrawer()
    private val target = RecordingLayerTarget()

    /** Rectangles are eight vertices each: four corners inside the fringe and four outside. */
    private val rectangleVertices = 8

    private fun draw(visibles: List<Visible>, pixelsPerUnit: Float = 1f, isTextureSampledPerVertex: Boolean = true) {
        target.events.clear()
        target.runBatches.clear()
        drawer.draw(visibles, target, pixelsPerUnit, isTextureSampledPerVertex)
    }

    @Test
    fun `batched, plain, and batched actors draw as two runs around one plain draw`() {
        val scene = listOf(
            TestBatched(RectangleLook(Color.Red)),
            TestBatched(RectangleLook(Color.Green)),
            TestPlain("middle"),
            TestBatched(RectangleLook(Color.Blue)),
        )

        draw(scene)

        assertEquals(listOf("run:${rectangleVertices * 2}", "plain:middle", "run:$rectangleVertices"), target.events)
    }

    @Test
    fun `invisible actors are left out`() {
        val hidden = TestBatched(RectangleLook()).apply { isVisible = false }

        draw(listOf(TestBatched(RectangleLook()), hidden))

        assertEquals(listOf("run:$rectangleVertices"), target.events)
    }

    @Test
    fun `a run rolls over before it outgrows the index range`() {
        val quadsPerActor = 1_000
        val actors = List(40) { TestBatched(QuadsLook(quadsPerActor)) }

        draw(actors)

        val runVertexCounts = target.events.map { it.removePrefix("run:").toInt() }
        assertEquals(40 * quadsPerActor * 4, runVertexCounts.sum())
        assertTrue(runVertexCounts.size > 1)
        assertTrue(runVertexCounts.all { it <= TriangleBatch.MAX_INDEXED_VERTICES })
    }

    @Test
    fun `nothing is refilled while nothing changed`() {
        val looks = List(3) { QuadsLook(2) }
        val scene = looks.map { TestBatched(it) }
        draw(scene)
        val batch = target.runBatches.single()
        val generation = batch.generation

        repeat(3) { draw(scene) }

        assertSame(batch, target.runBatches.single())
        assertEquals(generation, batch.generation)
        assertTrue(looks.all { it.emitCount == 1 })
    }

    @Test
    fun `a moved actor rebuilds only its own geometry and the run is filled on from it`() {
        val looks = List(3) { QuadsLook(2) }
        val scene = looks.map { TestBatched(it) }
        draw(scene)
        val batch = target.runBatches.single()
        val generation = batch.generation

        scene[1].body.position = SceneOffset(5f.sceneUnit, 0f.sceneUnit)
        draw(scene)

        assertEquals(generation, batch.generation, "The unchanged first member is kept rather than appended again.")
        assertEquals(listOf(1, 2, 1), looks.map { it.emitCount })
        assertEquals(24, batch.vertexCount)
        assertEquals(-5f, batch.positions[0], "The first member, which did not move, keeps its vertices.")
        assertEquals(0f, batch.positions[8 * 2], "The moved actor's top-left corner, which starts at its pivot's negative, follows it.")
        assertEquals(-5f, batch.positions[16 * 2], "The third member is appended again after the moved one.")
    }

    @Test
    fun `actors sharing one look each draw where they are`() {
        val look = QuadsLook(1)
        val first = TestBatched(look)
        val second = TestBatched(look).apply { body.position = SceneOffset(100f.sceneUnit, 0f.sceneUnit) }

        repeat(2) {
            draw(listOf(first, second))

            val batch = target.runBatches.single()
            assertEquals(-5f, batch.positions[0])
            assertEquals(95f, batch.positions[4 * 2])
        }
    }

    @Test
    fun `a reordered run is refilled and a shrunk one cut back`() {
        val first = TestBatched(QuadsLook(1))
        val second = TestBatched(QuadsLook(1))
        draw(listOf(first, second))
        val batch = target.runBatches.single()

        var generation = batch.generation
        draw(listOf(second, first))
        assertNotEquals(generation, batch.generation)

        draw(listOf(second))
        assertEquals(4, batch.vertexCount)
        assertEquals(listOf("run:4"), target.events)
    }

    @Test
    fun `a changed look rebuilds its geometry`() {
        val look = RectangleLook(Color.Red)
        val scene = listOf(TestBatched(look))
        draw(scene)

        look.color = Color.Blue
        draw(scene)

        assertEquals(Color.Blue.value.shr(32).toInt(), target.runBatches.single().colors[0])
    }

    @Test
    fun `actors of one drawing order are grouped by texture`() {
        val first = ImageBitmap(4, 4)
        val second = ImageBitmap(4, 4)
        val scene = listOf(
            TestBatched(SpriteLook(first)),
            TestBatched(SpriteLook(second)),
            TestBatched(SpriteLook(first)),
            TestBatched(RectangleLook()),
        )

        draw(scene)

        assertEquals(listOf("run:16", "run:8", "run:8"), target.events)
        assertSame(first, target.runBatches[0].texture)
        assertSame(second, target.runBatches[1].texture)
        assertNull(target.runBatches[2].texture)
    }

    @Test
    fun `sprites showing different regions of one atlas page share a run`() {
        val page = ImageBitmap(8, 4)
        val scene = listOf(
            TestBatched(SpriteLook(page, srcOffset = IntOffset(0, 0), srcSize = IntSize(4, 4))),
            TestBatched(SpriteLook(page, srcOffset = IntOffset(4, 0), srcSize = IntSize(4, 4))),
        )

        draw(scene)

        assertEquals(listOf("run:${rectangleVertices * 2}"), target.events)
        val batch = target.runBatches.single()
        assertSame(page, batch.texture)
        assertEquals(0f, batch.texCoords[0])
        assertEquals(4f, batch.texCoords[rectangleVertices * 2])
    }

    @Test
    fun `looks sampling one texture with different defaults for untextured vertices take runs of their own`() {
        val texture = ImageBitmap(4, 4)
        val scene = listOf(
            TestBatched(TexturedQuadsLook(texture, defaultU = 1f)),
            TestBatched(TexturedQuadsLook(texture, defaultU = 3f)),
            TestBatched(TexturedQuadsLook(texture, defaultU = 1f)),
        )

        draw(scene)

        assertEquals(listOf("run:8", "run:4"), target.events)
        assertEquals(1f, target.runBatches[0].defaultU)
        assertEquals(3f, target.runBatches[1].defaultU)
    }

    @Test
    fun `actors of different drawing orders keep their order across textures`() {
        val first = ImageBitmap(4, 4)
        val second = ImageBitmap(4, 4)
        val scene = listOf(
            TestBatched(SpriteLook(first), drawingOrder = 3f),
            TestBatched(SpriteLook(second), drawingOrder = 2f),
            TestBatched(SpriteLook(first), drawingOrder = 1f),
        )

        draw(scene)

        assertEquals(listOf("run:8", "run:8", "run:8"), target.events)
    }

    @Test
    fun `sprites are drawn one by one where textures are not sampled per vertex`() {
        val texture = ImageBitmap(4, 4)
        val scene = listOf(TestBatched(SpriteLook(texture)), TestBatched(SpriteLook(texture)), TestBatched(EllipseLook()))

        draw(scene, isTextureSampledPerVertex = false)

        assertEquals("sprite", target.events[0])
        assertEquals("sprite", target.events[1])
        assertTrue(target.events[2].startsWith("run:"))
    }

    @Test
    fun `a sprite too small to show its texture joins the untextured run in its mean color`() {
        val texture = ImageBitmap(4, 4)
        val scene = listOf(TestBatched(SpriteLook(texture, tint = Color.Red), size = 2f), TestBatched(RectangleLook(Color.Green), size = 2f))

        draw(scene)

        assertEquals(listOf("run:${rectangleVertices * 2}"), target.events)
        assertNull(target.runBatches.single().texture)
    }

    @Test
    fun `a stand-in takes the mean color of its own region, not of the whole texture`() {
        val texture = ImageBitmap(2, 1).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawRect(0f, 0f, 1f, 1f, Paint().apply { color = Color.Red })
            canvas.drawRect(1f, 0f, 2f, 1f, Paint().apply { color = Color.Green })
        }
        val look = SpriteLook(texture, srcOffset = IntOffset(1, 0), srcSize = IntSize(1, 1))

        draw(listOf(TestBatched(look, size = 2f)))

        assertEquals(Color.Green.toArgb(), target.runBatches.single().colors[0])
    }

    @Test
    fun `a sprite without a texture draws nothing`() {
        draw(listOf(TestBatched(SpriteLook(texture = null)), TestPlain("after")))

        assertEquals(listOf("plain:after"), target.events)
    }
}
