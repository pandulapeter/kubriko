/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.endToEnd

import com.pandulapeter.kubrikoCreator.context.BreakoutFixture
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.boundsOf
import com.pandulapeter.kubrikoCreator.endToEnd.ImageChecks.countOf
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * The harness's own scenario: Breakout opens in Kubriko Creator, builds, and plays, drawing its Actors where its Scene
 * places them. Then shapes drawn through the batched looks every generated Actor type has look the same as when they are
 * drawn plain, as the engine's `BatchedRenderingComparisonTest` checks for the engine alone: here through a generated
 * game, its build, and its viewport. Twins of the shape types make themselves invisible and draw the shape plain instead,
 * through a Kotlin Block, so the two Scenes differ only in how their shapes are drawn.
 */
class OpeningAndPlayingTest {

    @Test
    fun breakoutOpensBuildsAndPlaysWithItsActorsWhereTheSceneHasThem() = EndToEnd.scenario("opening-and-playing") {
        step("Kubriko Creator's window shows") {
            fun drawnPixels(screen: BufferedImage) = screen.width * screen.height - screen.countOf(BLACK, tolerance = 0)
            val screen = awaitScreen("creator") { drawnPixels(it) >= MINIMUM_WINDOW_AREA }
            assertTrue(drawnPixels(screen) >= MINIMUM_WINDOW_AREA, "Only ${drawnPixels(screen)} pixels of the screen were drawn, less than Kubriko Creator's smallest window.")
        }
        step("Kubriko Creator keeps its settings in the scenario's folder") {
            assertTrue(File(outputDirectory, "settings/recent-projects.json").readText().contains(project.name))
        }
        step("start_game builds and starts the game") {
            val started = tools.call("start_game", timeout = 10.minutes)
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
        }
        val first = step("take_screenshot shows the game") { captureGame("game") }
        // take_screenshot scales the game down to at most 1280 pixels wide, so how many pixels a unit is depends on the display.
        val scale = step("The game draws the Gold Bricks in their column") {
            val gold = assertNotNull(first.boundsOf(GOLD), "No Gold Brick was drawn.")
            // The level's rows are 30 units apart, which the last two rows show even when a short window cuts the first off.
            val rowTops = rowTopsAt(first, gold.centerX.toInt())
            assertTrue(rowTops.size >= 3, "Only ${rowTops.size} rows of Gold Bricks were drawn in their column.")
            val scale = (rowTops.last() - rowTops[rowTops.lastIndex - 1]) / ROW_SPACING
            assertTrue(abs(gold.width - BreakoutFixture.brick.width * scale) <= 2, "The Gold Bricks are ${gold.width} pixels wide (at $scale pixels a unit).")
            // The level puts its Gold Bricks in the fifth column, whose middle is 30 units left of the screen's.
            assertTrue(abs(gold.centerX - (first.width / 2 - 30 * scale)) <= 2, "The Gold Bricks are at $gold, not in their column (at $scale pixels a unit).")
            assertTrue(first.countOf(BRICK) > first.countOf(GOLD) * 5, "The other Bricks weren't drawn around them.")
            val ball = assertNotNull(first.boundsOf(BALL), "The Ball wasn't drawn.")
            val ballSize = BreakoutFixture.ball.width * scale
            assertTrue(abs(ball.width - ballSize) <= 2 && abs(ball.height - ballSize) <= 2, "The Ball is drawn ${ball.width}×${ball.height} pixels large (at $scale pixels a unit).")
            scale
        }
        step("The Game's two texts at the top left are one under the other") {
            val text = assertNotNull(first.boundsOf(TEXT), "The Game's texts weren't drawn.")
            val lineHeight = BreakoutFixture.game.texts.first().size * scale
            assertTrue(text.height > lineHeight * 1.5f, "The texts are only ${text.height} pixels tall, so they are drawn over each other.")
        }
        step("The Ball moves while the rest stays") {
            val second = captureGame("game-later")
            val difference = ImageChecks.differenceBetween(first, second)
            assertTrue(difference > 0f && difference < 0.05f, "${difference * 100}% of the game changed between two screenshots.")
        }
        step("Shapes, and twins that draw them plain, are placed alike in two Scenes") {
            succeed("stop_game")
            listOf(TILE, BUBBLE).forEach { type ->
                succeed("create_actor_type", type.definition())
                succeed("create_actor_type", type.definition(name = PLAIN_PREFIX + type.name, events = listOf(type.drawnPlain())))
            }
            listOf(BATCHED_SCENE to "", PLAIN_SCENE to PLAIN_PREFIX).forEach { (scene, prefix) ->
                succeed("create_scene", mapOf("scene" to scene))
                succeed("place_actors", mapOf("scene" to scene, "actors" to shapes.mapIndexed { index, shape -> mapOf("actorType" to prefix + shape.type.name, "x" to shape.x, "y" to shape.y, "actorId" to "shape$index") }))
                succeed("set_actors_values", mapOf("scene" to scene, "actors" to shapes.mapIndexed { index, shape -> mapOf("actorId" to "shape$index", "width" to shape.width, "height" to shape.height, "rotation" to shape.rotation) }))
            }
        }
        val batched = step("The Scene of shapes drawn through their batched looks plays") { playShapes(BATCHED_SCENE, scale) }
        val plain = step("The Scene of shapes drawn plain plays") { playShapes(PLAIN_SCENE, scale) }
        step("Both draw the shapes alike, apart from how their edges are smoothed") {
            val drawn = batched.countOf(TILE.color) + batched.countOf(BUBBLE.color)
            val expected = (TILE.areaAt(scale) + BUBBLE.areaAt(scale)).roundToInt()
            assertTrue(drawn >= expected * MINIMUM_DRAWN_SHARE, "Only $drawn pixels of the shapes were drawn, of about $expected (at $scale pixels a unit).")
            val differing = (ImageChecks.differenceBetween(batched, plain, tolerance = EDGE_TOLERANCE) * batched.width * batched.height).roundToInt()
            assertTrue(differing < drawn / MAXIMUM_DIFFERING_SHARE, "$differing pixels differ between the shapes drawn batched and plain, of $drawn drawn.")
        }
    }

    /**
     * What the game shows started in [scene], once it draws both shapes at [scale] pixels a unit, stopping it afterwards.
     */
    private fun EndToEndScenario.playShapes(scene: String, scale: Float): BufferedImage {
        val started = tools.call("start_game", mapOf("scene" to scene), timeout = 10.minutes)
        try {
            assertTrue(!started.isError && started["hasStarted"]?.jsonPrimitive?.boolean == true, started.text)
            fun drawsBoth(image: BufferedImage) = listOf(TILE, BUBBLE).all { image.countOf(it.color) >= it.areaAt(scale) * MINIMUM_DRAWN_SHARE }
            return awaitGame(scene, isReady = ::drawsBoth).also { assertTrue(drawsBoth(it), "The game started in $scene never drew both shapes.") }
        } finally {
            // Stopping can fail too once the game is gone, which mustn't hide why the step failed.
            runCatching { tools.call("stop_game") }
        }
    }

    /**
     * A block-built Actor type drawn as [shape] in [color] (RGB).
     */
    private class ShapeType(val name: String, val shape: String, val color: Int, val plainDrawing: String) {
        /**
         * How many pixels the [shapes] of this type cover at [scale] pixels a unit.
         */
        fun areaAt(scale: Float) = shapes.filter { it.type === this }.sumOf { shape ->
            (shape.width * shape.height * (if (this.shape == "circle") PI / 4 else 1.0)).toDouble()
        }.toFloat() * scale * scale

        fun definition(name: String = this.name, events: List<Map<String, Any?>> = emptyList()) =
            mapOf("name" to name, "shape" to shape, "color" to "#%06X".format(color), "events" to events)

        /**
         * The added-to-the-Scene Event of a twin that hides its batched look and adds an Actor drawing the shape plain
         * with the same body, layer, and drawing order, so it is placed, turned, stretched, and drawn in order alike.
         */
        fun drawnPlain() = mapOf(
            "type" to "addedToScene",
            "blocks" to listOf(
                mapOf(
                    "type" to "kotlin",
                    "code" to """
                        import androidx.compose.ui.graphics.Color
                        import androidx.compose.ui.graphics.drawscope.DrawScope
                        import com.pandulapeter.kubriko.actor.Actor
                        import com.pandulapeter.kubriko.actor.traits.Visible
                        val sharedBody = body
                        val sharedLayerIndex = layerIndex
                        val sharedDrawingOrder = drawingOrder
                        val shown = color
                        color = Color.Transparent
                        actorManager.add(
                            object : Actor, Visible {
                                override val body = sharedBody
                                override val layerIndex = sharedLayerIndex
                                override val drawingOrder = sharedDrawingOrder
                                override fun DrawScope.draw() = $plainDrawing(shown, size = body.size.raw)
                            },
                        )
                    """.trimIndent(),
                ),
            ),
        )
    }

    private class PlacedShape(val type: ShapeType, val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float)

    /**
     * Where each run of Gold Brick pixels starts, from the top, in the column of pixels at [x].
     */
    private fun rowTopsAt(image: BufferedImage, x: Int) = (0 until image.height).filter { y ->
        ImageChecks.matches(image.getRGB(x, y), GOLD) && (y == 0 || !ImageChecks.matches(image.getRGB(x, y - 1), GOLD))
    }

    private companion object {
        val TILE = ShapeType("Tile", shape = "rectangle", color = 0xCC3399, plainDrawing = "drawRect")
        val BUBBLE = ShapeType("Bubble", shape = "circle", color = 0x33BBCC, plainDrawing = "drawOval")

        /**
         * A rectangle and a circle as they are, and each turned and stretched, which smooths their edges the most.
         */
        val shapes = listOf(
            PlacedShape(TILE, x = -200f, y = -90f, width = 140f, height = 70f, rotation = 0f),
            PlacedShape(TILE, x = 160f, y = -90f, width = 140f, height = 70f, rotation = 20f),
            PlacedShape(BUBBLE, x = -200f, y = 110f, width = 110f, height = 110f, rotation = 0f),
            PlacedShape(BUBBLE, x = 160f, y = 110f, width = 170f, height = 80f, rotation = -30f),
        )
        const val PLAIN_PREFIX = "Plain"
        const val BATCHED_SCENE = "batched-shapes"
        const val PLAIN_SCENE = "plain-shapes"

        /**
         * How much of the shapes' area has to show in their color: all but their smoothed edges.
         */
        const val MINIMUM_DRAWN_SHARE = 0.8f

        /**
         * How far a pixel along an edge may differ, as in `BatchedRenderingComparisonTest`: the batch's fringe and Skia's
         * anti-aliasing smooth edges a little differently, but never by a whole color.
         */
        const val EDGE_TOLERANCE = 64

        /**
         * At most one in this many of the shapes' pixels may differ by more than [EDGE_TOLERANCE].
         */
        const val MAXIMUM_DIFFERING_SHARE = 50
        const val ROW_SPACING = 30f
        const val BLACK = 0x000000
        const val GOLD = 0xFFC857
        const val BRICK = 0x6060AA
        const val BALL = 0xFFFFFF
        const val TEXT = 0x202020
        /**
         * The smallest window Kubriko Creator lets itself be (KubrikoCreatorApp.kt), at the display's scale of 1.
         */
        const val MINIMUM_WINDOW_AREA = 960 * 600
    }
}
