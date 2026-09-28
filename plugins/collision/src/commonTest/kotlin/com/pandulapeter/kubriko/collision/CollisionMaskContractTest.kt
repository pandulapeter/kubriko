/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.extensions.collisionResultWith
import com.pandulapeter.kubriko.collision.extensions.hasCollisionWith
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PointCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollisionMaskContractTest {

    @Test
    fun clearPlacementsAreSymmetricAndMatchTheExpectedValue() = forEveryOrderedPair { first, second ->
        Placement.entries.forEach { placement ->
            val (a, b) = place(first, second, placement, SceneOffset.Zero)
            val expected = placement == Placement.OVERLAPPING && !(first.isPoint && second.isPoint)
            assertEquals(expected, a.hasCollisionWith(b), "${first.name} vs ${second.name}, $placement")
            assertEquals(expected, b.hasCollisionWith(a), "${second.name} vs ${first.name}, $placement")
        }
    }

    @Test
    fun exactTouchesAgreeInBothOrders() {
        val touchingPairs = listOf(
            CIRCLE to CIRCLE,
            CIRCLE to BOX,
            BOX to CIRCLE,
            BOX to BOX,
        )
        touchingPairs.forEach { (first, second) ->
            val a = first.create(SceneOffset.Zero)
            val b = second.create(offset(first.halfWidth + second.halfWidth, 0f))
            assertEquals(a.hasCollisionWith(b), b.hasCollisionWith(a), "${first.name} touching ${second.name}")
        }
    }

    @Test
    fun translationDoesNotChangeClearPlacements() = forEveryOrderedPair { first, second ->
        Placement.entries.forEach { placement ->
            val (a, b) = place(first, second, placement, SceneOffset.Zero)
            val (shiftedA, shiftedB) = place(first, second, placement, offset(12_345f, -6_789f))
            assertEquals(a.hasCollisionWith(b), shiftedA.hasCollisionWith(shiftedB), "${first.name} vs ${second.name}, $placement")
        }
    }

    @Test
    fun squareRotatedByAQuarterTurnBehavesLikeTheUnrotatedSquare() = CATALOGUE.forEach { other ->
        Placement.entries.forEach { placement ->
            val (square, otherForSquare) = place(SQUARE, other, placement, SceneOffset.Zero)
            val (rotatedSquare, otherForRotatedSquare) = place(ROTATED_SQUARE, other, placement, SceneOffset.Zero)
            assertEquals(square.hasCollisionWith(otherForSquare), rotatedSquare.hasCollisionWith(otherForRotatedSquare), "${other.name}, $placement")
            assertEquals(otherForSquare.hasCollisionWith(square), otherForRotatedSquare.hasCollisionWith(rotatedSquare), "${other.name}, $placement")
        }
    }

    @Test
    fun boundingBoxContainsTheWholeMask() = CATALOGUE.forEach { factory ->
        val position = offset(7f, -3f)
        val mask = factory.create(position)
        val boundingBox = mask.axisAlignedBoundingBox
        when (mask) {
            is PolygonCollisionMask -> mask.vertices.forEach { vertex ->
                val worldVertex = position + mask.rotationMatrix.times(vertex)
                assertTrue(
                    worldVertex.x.raw in boundingBox.minXRaw..boundingBox.maxXRaw && worldVertex.y.raw in boundingBox.minYRaw..boundingBox.maxYRaw,
                    "${factory.name}: $worldVertex is outside its bounding box",
                )
            }

            is CircleCollisionMask -> repeat(16) { step ->
                val angle = step * 2 * PI / 16
                val x = position.x.raw + mask.radius.raw * cos(angle).toFloat()
                val y = position.y.raw + mask.radius.raw * sin(angle).toFloat()
                assertTrue(
                    x in boundingBox.minXRaw - CIRCLE_PADDING..boundingBox.maxXRaw + CIRCLE_PADDING &&
                            y in boundingBox.minYRaw - CIRCLE_PADDING..boundingBox.maxYRaw + CIRCLE_PADDING,
                    "${factory.name}: ($x, $y) is outside its bounding box",
                )
            }

            else -> {
                assertEquals(position, boundingBox.min)
                assertEquals(position, boundingBox.max)
            }
        }
    }

    @Test
    fun collisionResultIsPresentExactlyWhenMasksCollideAndIsWellFormed() = forEveryOrderedPair { first, second ->
        Placement.entries.forEach { placement ->
            val (a, b) = place(first, second, placement, SceneOffset.Zero)
            val result = a.collisionResultWith(b, shouldSkipAxisAlignedBoundingBoxCheck = false)
            val message = "${first.name} vs ${second.name}, $placement"
            if (a.hasCollisionWith(b)) {
                assertNotNull(result, message)
                assertTrue(result.contact.x.raw.isFinite() && result.contact.y.raw.isFinite(), "$message: contact ${result.contact}")
                val normalLength = sqrt(result.contactNormal.x.raw * result.contactNormal.x.raw + result.contactNormal.y.raw * result.contactNormal.y.raw)
                assertTrue(abs(normalLength - 1f) <= 1e-3f, "$message: normal ${result.contactNormal}")
                assertTrue(result.penetration.raw >= 0f, "$message: penetration ${result.penetration}")
            } else {
                assertNull(result, message)
            }
        }
    }

    private enum class Placement { OVERLAPPING, SEPARATED }

    internal class MaskFactory(
        val name: String,
        val halfWidth: Float,
        val isPoint: Boolean = false,
        val create: (SceneOffset) -> CollisionMask,
    )

    private fun forEveryOrderedPair(block: (MaskFactory, MaskFactory) -> Unit) = CATALOGUE.forEach { first ->
        CATALOGUE.forEach { second -> block(first, second) }
    }

    private fun place(first: MaskFactory, second: MaskFactory, placement: Placement, shift: SceneOffset) = first.create(shift) to second.create(
        shift + when (placement) {
            Placement.OVERLAPPING -> SceneOffset.Zero
            Placement.SEPARATED -> offset(SEPARATION, 0f)
        }
    )

    companion object {
        private const val SEPARATION = 60f
        private const val CIRCLE_PADDING = 0.5f

        private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

        private val CIRCLE = MaskFactory("circle", halfWidth = 10f) { CircleCollisionMask(initialPosition = it, initialRadius = 10f.sceneUnit) }
        private val BOX = MaskFactory("box", halfWidth = 10f) { BoxCollisionMask(initialPosition = it, initialSize = SceneSize(20f.sceneUnit, 10f.sceneUnit)) }
        private val SQUARE = MaskFactory("square", halfWidth = 8f) { BoxCollisionMask(initialPosition = it, initialSize = SceneSize(16f.sceneUnit, 16f.sceneUnit)) }
        private val ROTATED_SQUARE = MaskFactory("square rotated by π/2", halfWidth = 8f) {
            BoxCollisionMask(initialPosition = it, initialSize = SceneSize(16f.sceneUnit, 16f.sceneUnit), initialRotation = (PI / 2).toFloat().rad)
        }

        /** Every mask kind the contract covers. The masks are mutable, so every placement builds fresh ones. */
        internal val CATALOGUE = listOf(
            CIRCLE,
            BOX,
            MaskFactory("box rotated by π/6", halfWidth = 11.2f) {
                BoxCollisionMask(initialPosition = it, initialSize = SceneSize(20f.sceneUnit, 10f.sceneUnit), initialRotation = (PI / 6).toFloat().rad)
            },
            SQUARE,
            MaskFactory("triangle", halfWidth = 12f) {
                PolygonCollisionMask(vertices = listOf(offset(-10f, 8f), offset(10f, 8f), offset(0f, -12f)), initialPosition = it)
            },
            MaskFactory("pentagon", halfWidth = 10f) { position ->
                PolygonCollisionMask(
                    vertices = List(5) { index ->
                        val angle = index * 2 * PI / 5
                        offset(10f * cos(angle).toFloat(), 10f * sin(angle).toFloat())
                    },
                    initialPosition = position,
                )
            },
            MaskFactory("point", halfWidth = 0f, isPoint = true) { PointCollisionMask(it) },
        )
    }
}
