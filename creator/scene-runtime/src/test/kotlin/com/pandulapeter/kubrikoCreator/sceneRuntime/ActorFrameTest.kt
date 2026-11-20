/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActorFrameTest {
    private val body = BoxBody(
        initialPosition = SceneOffset(100f.sceneUnit, 50f.sceneUnit),
        initialSize = SceneSize(40f.sceneUnit, 20f.sceneUnit),
        initialPivot = SceneOffset(10f.sceneUnit, 5f.sceneUnit),
        initialScale = Scale(2f, 1.5f),
        initialRotation = (PI / 6).toFloat().rad,
    )
    private val frame = ActorFrame.of(body)

    @Test
    fun theCornersAreWhereKubrikoPutsTheBox() {
        val box = body.axisAlignedBoundingBox
        val corners = listOf(FrameHandle.TOP_LEFT, FrameHandle.TOP_RIGHT, FrameHandle.BOTTOM_RIGHT, FrameHandle.BOTTOM_LEFT).map(frame::pointOf)
        assertNear(box.min.x.raw, corners.minOf { it.x })
        assertNear(box.min.y.raw, corners.minOf { it.y })
        assertNear(box.max.x.raw, corners.maxOf { it.x })
        assertNear(box.max.y.raw, corners.maxOf { it.y })
        assertNear(Offset(100f, 50f), frame.toScene(Offset(10f, 5f)))
    }

    @Test
    fun aPointOfTheSceneGoesBackToWhereItIsInTheBox() {
        val inBox = Offset(33f, 7f)
        assertNear(inBox, frame.toBox(frame.toScene(inBox)))
    }

    @Test
    fun resizingKeepsTheOppositeEdgeWhereItIsAndThePivotInProportion() {
        val left = frame.pointOf(FrameHandle.LEFT)
        val resized = frame.resized(FrameHandle.RIGHT, frame.toScene(Offset(60f, 3f)))
        assertNear(Offset(60f, 20f), resized.size)
        assertNear(Offset(15f, 5f), resized.pivot)
        assertNear(left, resized.pointOf(FrameHandle.LEFT))
        assertNear(frame.pointOf(FrameHandle.TOP_LEFT), resized.pointOf(FrameHandle.TOP_LEFT))
    }

    @Test
    fun aCornerDraggedPastItsOppositeOneLeavesTheSmallestBoxThere() {
        val bottomRight = frame.pointOf(FrameHandle.BOTTOM_RIGHT)
        val resized = frame.resized(FrameHandle.TOP_LEFT, frame.toScene(Offset(80f, 80f)))
        assertNear(Offset(ActorFrame.MINIMUM_SIZE, ActorFrame.MINIMUM_SIZE), resized.size)
        assertNear(bottomRight, resized.pointOf(FrameHandle.BOTTOM_RIGHT))
    }

    @Test
    fun aCornerCanKeepItsProportions() {
        val resized = frame.resized(FrameHandle.BOTTOM_RIGHT, frame.toScene(Offset(80f, 25f)), keepsProportions = true)
        assertNear(Offset(80f, 40f), resized.size)
    }

    @Test
    fun snappingRoundsOnlyTheEdgesTheHandleMoves() {
        val resized = frame.resized(FrameHandle.BOTTOM, frame.toScene(Offset(3f, 27f)), snap = { (it / 8f).toInt() * 8f })
        assertNear(Offset(40f, 24f), resized.size)
    }

    @Test
    fun anUnrotatedBoxsDraggedCornerLandsOnTheGridAndKeepsItsProportionsWithShift() {
        val unrotated = ActorFrame(position = Offset(5f, 5f), size = Offset(10f, 30f), pivot = Offset(5f, 15f))
        val grid = { value: Float -> Math.round(value / 16f) * 16f }
        val snapped = unrotated.resized(FrameHandle.BOTTOM_RIGHT, Offset(29f, 35f), snap = grid)
        assertNear(Offset(32f, 32f), snapped.pointOf(FrameHandle.BOTTOM_RIGHT))
        val proportional = unrotated.resized(FrameHandle.BOTTOM_RIGHT, Offset(14f, 33f), keepsProportions = true, snap = grid)
        assertNear(1f / 3f, proportional.size.x / proportional.size.y)
    }

    @Test
    fun aRotatedBoxKeepingItsProportionsSnapsOnlyItsLeadingSide() {
        val tall = frame.copy(size = Offset(10f, 30f), pivot = Offset(5f, 15f))
        val resized = tall.resized(FrameHandle.BOTTOM_RIGHT, tall.toScene(Offset(13f, 39f)), keepsProportions = true, snap = { Math.round(it / 16f) * 16f })
        assertNear(Offset(16f, 48f), resized.size)
    }

    @Test
    fun rotatingTurnsAroundThePivotAndCanGoInSteps() {
        val pivot = frame.position
        val rotated = frame.rotated(pivot + Offset(10f, 0f), pivot + Offset(0f, 10f))
        assertNear((PI / 6 + PI / 2).toFloat(), rotated.rotation)
        assertEquals(frame.position, rotated.position)
        val stepped = frame.rotated(pivot + Offset(10f, 0f), pivot + Offset(10f, 1f), stepInDegrees = 15f)
        assertEquals(30f, ActorFrame.degreesOf(stepped.rotation))
    }

    @Test
    fun appliedToABodyItGivesItEveryValue() {
        val other = BoxBody()
        frame.resized(FrameHandle.TOP, frame.toScene(Offset(0f, -10f))).applyTo(other)
        val read = ActorFrame.of(other)
        assertNear(Offset(40f, 30f), read.size)
        assertNear(frame.pointOf(FrameHandle.BOTTOM_LEFT), read.pointOf(FrameHandle.BOTTOM_LEFT))
    }

    @Test
    fun aBoxScaledToNothingCantBeTurnedBack() {
        assertTrue(frame.isInvertible)
        assertFalse(frame.copy(scaleHorizontal = 0f).isInvertible)
        assertFalse(frame.copy(size = Offset(0f, 3f)).isInvertible)
    }

    @Test
    fun degreesAreBetweenZeroAndAFullTurn() {
        assertEquals(0f, ActorFrame.degreesOf((2 * PI).toFloat()))
        assertEquals(270f, ActorFrame.degreesOf((-PI / 2).toFloat()))
    }

    private fun assertNear(expected: Float, actual: Float) = assertEquals(expected, actual, absoluteTolerance = 0.01f)

    private fun assertNear(expected: Offset, actual: Offset) {
        assertNear(expected.x, actual.x)
        assertNear(expected.y, actual.y)
    }
}
