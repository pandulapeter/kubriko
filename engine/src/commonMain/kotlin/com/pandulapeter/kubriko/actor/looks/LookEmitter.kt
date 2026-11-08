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

import com.pandulapeter.kubriko.helpers.TriangleBatch
import com.pandulapeter.kubriko.helpers.transformVertices
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Turns looks into triangles. Every declarative look is a convex outline (four corners, or an ellipse's segments)
 * mapped into the scene by a [LookTransform], filled as a fan and ringed by a fringe one screen pixel wide that fades
 * to transparent: the batch is drawn without anti-aliasing, and the fringe is what smooths its edges the way
 * anti-aliased canvas drawing does. Half of the fringe lies inside the outline and half outside, so the shape keeps
 * the size it reads as. Scratch buffers are reused, so emitting allocates nothing; drawing-thread only.
 */
internal object LookEmitter {

    private const val MINIMUM_ELLIPSE_SEGMENTS = 8
    private const val MAXIMUM_ELLIPSE_SEGMENTS = 256

    /** How far, in screen pixels, an ellipse's segments may stray from its true outline. */
    private const val ELLIPSE_TOLERANCE_IN_PIXELS = 0.25f

    /** Caps how far a corner's fringe reaches along its bisector, so a sliver of a corner cannot shoot off. */
    private const val MINIMUM_HALF_ANGLE_COSINE = 0.25f

    private val ringX = FloatArray(MAXIMUM_ELLIPSE_SEGMENTS)
    private val ringY = FloatArray(MAXIMUM_ELLIPSE_SEGMENTS)
    private val ringU = FloatArray(4)
    private val ringV = FloatArray(4)
    private val edgeNormalX = FloatArray(MAXIMUM_ELLIPSE_SEGMENTS)
    private val edgeNormalY = FloatArray(MAXIMUM_ELLIPSE_SEGMENTS)

    /**
     * Emits [look] over a body of [width] × [height] into [batch], mapped by [transform]. [pixelsPerUnit] is the screen
     * pixels per scene unit, which sizes the fringes and the ellipse segments. [isStandIn] draws a sprite in its
     * texture's mean color.
     */
    fun emit(
        look: Look,
        batch: TriangleBatch,
        width: Float,
        height: Float,
        transform: LookTransform,
        pixelsPerUnit: Float,
        isStandIn: Boolean,
    ) {
        if (width <= 0f || height <= 0f || pixelsPerUnit <= 0f) return
        val halfFringe = 0.5f / pixelsPerUnit
        when (look) {
            is RectangleLook -> {
                setRectangleRing(width, height, transform)
                emitRing(batch, 4, look.argb, halfFringe, isTextured = false)
            }
            is EllipseLook -> {
                val segmentCount = ellipseSegmentCount(width, height, transform, pixelsPerUnit)
                setEllipseRing(width, height, transform, segmentCount)
                emitRing(batch, segmentCount, look.argb, halfFringe, isTextured = false)
            }
            is SpriteLook -> emitSprite(look, batch, width, height, transform, halfFringe, isStandIn)
            is CustomLook -> {
                val firstVertex = batch.vertexCount
                val scale = sqrt(abs(transform.a * transform.d - transform.b * transform.c))
                look.emit(batch, width, height, pixelsPerUnit * scale)
                batch.transformVertices(firstVertex, transform.a, transform.b, transform.c, transform.d, transform.tx, transform.ty)
            }
        }
    }

    private fun emitSprite(
        look: SpriteLook,
        batch: TriangleBatch,
        width: Float,
        height: Float,
        transform: LookTransform,
        halfFringe: Float,
        isStandIn: Boolean,
    ) {
        val texture = look.texture ?: return
        setRectangleRing(width, height, transform)
        if (isStandIn) {
            emitRing(batch, 4, modulateArgb(TextureMeanColors.argbOf(texture), look.tintArgb), halfFringe, isTextured = false)
            return
        }
        val left = look.srcOffset.x.toFloat()
        val top = look.srcOffset.y.toFloat()
        val right = left + look.regionWidth(texture)
        val bottom = top + look.regionHeight(texture)
        ringU[0] = left; ringV[0] = top
        ringU[1] = right; ringV[1] = top
        ringU[2] = right; ringV[2] = bottom
        ringU[3] = left; ringV[3] = bottom
        emitRing(batch, 4, look.tintArgb, halfFringe, isTextured = true)
    }

    private fun setRectangleRing(width: Float, height: Float, transform: LookTransform) {
        ringX[0] = transform.x(0f, 0f); ringY[0] = transform.y(0f, 0f)
        ringX[1] = transform.x(width, 0f); ringY[1] = transform.y(width, 0f)
        ringX[2] = transform.x(width, height); ringY[2] = transform.y(width, height)
        ringX[3] = transform.x(0f, height); ringY[3] = transform.y(0f, height)
    }

    private fun setEllipseRing(width: Float, height: Float, transform: LookTransform, segmentCount: Int) {
        val radiusX = width * 0.5f
        val radiusY = height * 0.5f
        val step = 2.0 * PI / segmentCount
        for (i in 0 until segmentCount) {
            val angle = step * i
            val localX = radiusX + radiusX * cos(angle).toFloat()
            val localY = radiusY + radiusY * sin(angle).toFloat()
            ringX[i] = transform.x(localX, localY)
            ringY[i] = transform.y(localX, localY)
        }
    }

    /** Enough segments that no chord strays more than [ELLIPSE_TOLERANCE_IN_PIXELS] from the outline on screen. */
    internal fun ellipseSegmentCount(width: Float, height: Float, transform: LookTransform, pixelsPerUnit: Float): Int {
        val scaleX = sqrt(transform.a * transform.a + transform.b * transform.b)
        val scaleY = sqrt(transform.c * transform.c + transform.d * transform.d)
        val radiusInPixels = maxOf(width * scaleX, height * scaleY) * 0.5f * pixelsPerUnit
        if (radiusInPixels <= ELLIPSE_TOLERANCE_IN_PIXELS * 2f) return MINIMUM_ELLIPSE_SEGMENTS
        val segments = ceil(PI / acos(1.0 - ELLIPSE_TOLERANCE_IN_PIXELS / radiusInPixels)).toInt()
        return ((segments + 3) / 4 * 4).coerceIn(MINIMUM_ELLIPSE_SEGMENTS, MAXIMUM_ELLIPSE_SEGMENTS)
    }

    /**
     * Fills the convex ring of [count] points in [ringX]/[ringY] (and, [isTextured], [ringU]/[ringV]) with [argb], and
     * fringes it [halfFringe] in and out. A ring with no area emits nothing.
     */
    private fun emitRing(batch: TriangleBatch, count: Int, argb: Int, halfFringe: Float, isTextured: Boolean) {
        var doubleArea = 0f
        var centerX = 0f
        var centerY = 0f
        for (i in 0 until count) {
            val next = if (i + 1 == count) 0 else i + 1
            doubleArea += ringX[i] * ringY[next] - ringX[next] * ringY[i]
            centerX += ringX[i]
            centerY += ringY[i]
        }
        if (abs(doubleArea) < 1e-9f) return
        centerX /= count
        centerY /= count
        val outward = if (doubleArea > 0f) 1f else -1f
        var inradius = Float.MAX_VALUE
        for (i in 0 until count) {
            val next = if (i + 1 == count) 0 else i + 1
            val dx = ringX[next] - ringX[i]
            val dy = ringY[next] - ringY[i]
            val length = sqrt(dx * dx + dy * dy)
            if (length > 0f) {
                edgeNormalX[i] = dy / length * outward
                edgeNormalY[i] = -dx / length * outward
                inradius = minOf(inradius, (ringX[i] - centerX) * edgeNormalX[i] + (ringY[i] - centerY) * edgeNormalY[i])
            } else {
                edgeNormalX[i] = Float.NaN
                edgeNormalY[i] = Float.NaN
            }
        }
        val fringe = minOf(halfFringe, inradius * 0.5f)
        val transparent = argb and 0x00FFFFFF
        val first = batch.vertexCount
        for (i in 0 until count) {
            var offsetX = 0f
            var offsetY = 0f
            if (fringe > 0f) {
                val previous = previousEdgeWithNormal(i, count)
                val current = nextEdgeWithNormal(i, count)
                var bisectorX = edgeNormalX[previous] + edgeNormalX[current]
                var bisectorY = edgeNormalY[previous] + edgeNormalY[current]
                val bisectorLength = sqrt(bisectorX * bisectorX + bisectorY * bisectorY)
                if (bisectorLength < 1e-6f) {
                    bisectorX = edgeNormalX[current]
                    bisectorY = edgeNormalY[current]
                } else {
                    bisectorX /= bisectorLength
                    bisectorY /= bisectorLength
                }
                val halfAngleCosine = maxOf(bisectorX * edgeNormalX[current] + bisectorY * edgeNormalY[current], MINIMUM_HALF_ANGLE_COSINE)
                offsetX = bisectorX * fringe / halfAngleCosine
                offsetY = bisectorY * fringe / halfAngleCosine
            }
            addRingVertex(batch, ringX[i] - offsetX, ringY[i] - offsetY, argb, i, isTextured)
            if (fringe > 0f) addRingVertex(batch, ringX[i] + offsetX, ringY[i] + offsetY, transparent, i, isTextured)
        }
        val stride = if (fringe > 0f) 2 else 1
        for (i in 1 until count - 1) {
            batch.addTriangle(first, first + i * stride, first + (i + 1) * stride)
        }
        if (fringe > 0f) {
            for (i in 0 until count) {
                val next = if (i + 1 == count) 0 else i + 1
                batch.addQuad(first + i * 2, first + next * 2, first + next * 2 + 1, first + i * 2 + 1)
            }
        }
    }

    private fun addRingVertex(batch: TriangleBatch, x: Float, y: Float, argb: Int, ringIndex: Int, isTextured: Boolean) {
        if (isTextured) {
            batch.addTexturedVertex(x, y, argb, ringU[ringIndex], ringV[ringIndex])
        } else {
            batch.addVertex(x, y, argb)
        }
    }

    /** The edge ending at point [index] with a usable normal, skipping edges of zero length. */
    private fun previousEdgeWithNormal(index: Int, count: Int): Int {
        var edge = if (index == 0) count - 1 else index - 1
        repeat(count) {
            if (!edgeNormalX[edge].isNaN()) return edge
            edge = if (edge == 0) count - 1 else edge - 1
        }
        return edge
    }

    /** The edge starting at point [index] with a usable normal, skipping edges of zero length. */
    private fun nextEdgeWithNormal(index: Int, count: Int): Int {
        var edge = index
        repeat(count) {
            if (!edgeNormalX[edge].isNaN()) return edge
            edge = if (edge + 1 == count) 0 else edge + 1
        }
        return edge
    }
}
