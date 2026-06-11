/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.actor

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.Cuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.TriangleBatch
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.utility.TextureMipChain

open class VolumetricCuboidRenderer(
    val id: String,
    private val renderableCuboid: RenderableCuboid,
    private val textureResolver: (String) -> ImageBitmap?,
    initialWorldRotation: AngleRadians,
    initialWorldZoom: Float,
    initialWorldTilt: Float,
) : Visible {

    private val cuboid: Cuboid
        get() = renderableCuboid.cuboid

    override val body = BoxBody()
    override var drawingOrder = 0f

    private var isTranslucent = false

    private val faceColors: Array<Color> = Array(6) { Color.White }
    private val faceArgb = IntArray(6)
    private val visibleFaceId: IntArray = IntArray(6)
    private var visibleCount: Int = 0
    private var currentZoom = 1f

    private val texP0x = FloatArray(6);
    private val texP0y = FloatArray(6)
    private val texP1x = FloatArray(6);
    private val texP1y = FloatArray(6)
    private val texP2x = FloatArray(6);
    private val texP2y = FloatArray(6)
    private val texP3x = FloatArray(6);
    private val texP3y = FloatArray(6)

    private val cubeX = FloatArray(8)
    private val cubeY = FloatArray(8)
    private val cubeZ = FloatArray(8)

    private val camX = FloatArray(8)
    private val camY = FloatArray(8)
    private val camZ = FloatArray(8)

    private val projX = FloatArray(8)
    private val projY = FloatArray(8)

    private val faceDepth = FloatArray(6)
    private val faceOrder = IntArray(6)

    private var lastSizeXRaw: Float = Float.NaN
    private var lastSizeYRaw: Float = Float.NaN
    private var lastSizeZRaw: Float = Float.NaN
    private var lastRxRaw: Float = Float.NaN
    private var lastRyRaw: Float = Float.NaN
    private var lastRzRaw: Float = Float.NaN
    private var lastPosX: Float = Float.NaN
    private var lastPosY: Float = Float.NaN
    private var lastPosZ: Float = Float.NaN
    private var lastWorldRotRaw: Float = Float.NaN
    private var lastWorldZoom: Float = Float.NaN
    private var lastWorldTilt: Float = Float.NaN
    private var lastColorZMinus: Color? = null
    private var lastColorZPlus: Color? = null
    private var lastColorYMinus: Color? = null
    private var lastColorXPlus: Color? = null
    private var lastColorYPlus: Color? = null
    private var lastColorXMinus: Color? = null

    private val transformMatrix = Matrix()

    private val resolvedTextures = arrayOfNulls<TextureMipChain>(6)
    private val outlinePath = Path()
    private val outlineStroke = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Round)

    init {
        update(
            worldZoom = initialWorldZoom,
            worldRotation = initialWorldRotation,
            worldTilt = initialWorldTilt,
        )
    }

    // Returns true when anything that affects drawing changed since the previous call.
    fun update(
        worldRotation: AngleRadians,
        worldZoom: Float,
        worldTilt: Float,
        isTranslucent: Boolean = false,
    ): Boolean {
        val translucencyChanged = this.isTranslucent != isTranslucent
        this.isTranslucent = isTranslucent

        val sxRaw = cuboid.sizeX.raw
        val syRaw = cuboid.sizeY.raw
        val szRaw = cuboid.sizeZ.raw
        val rxRaw = renderableCuboid.rotationX.raw
        val ryRaw = renderableCuboid.rotationY.raw
        val rzRaw = renderableCuboid.rotationZ.raw
        val worldRotRaw = worldRotation.raw
        val worldCenterX = renderableCuboid.positionInWorld.x.raw
        val worldCenterY = renderableCuboid.positionInWorld.y.raw
        val worldCenterZ = renderableCuboid.positionInWorld.z.raw
        val colorZMinus = cuboid.colorZMinus
        val colorZPlus = cuboid.colorZPlus
        val colorYMinus = cuboid.colorYMinus
        val colorXPlus = cuboid.colorXPlus
        val colorYPlus = cuboid.colorYPlus
        val colorXMinus = cuboid.colorXMinus

        if (sxRaw == lastSizeXRaw && syRaw == lastSizeYRaw && szRaw == lastSizeZRaw
            && rxRaw == lastRxRaw && ryRaw == lastRyRaw && rzRaw == lastRzRaw
            && worldRotRaw == lastWorldRotRaw && worldZoom == lastWorldZoom && worldTilt == lastWorldTilt
            && worldCenterX == lastPosX && worldCenterY == lastPosY && worldCenterZ == lastPosZ
            && colorZMinus == lastColorZMinus && colorZPlus == lastColorZPlus
            && colorYMinus == lastColorYMinus && colorXPlus == lastColorXPlus
            && colorYPlus == lastColorYPlus && colorXMinus == lastColorXMinus) {
            return translucencyChanged
        }

        currentZoom = worldZoom
        lastRxRaw = rxRaw; lastRyRaw = ryRaw; lastRzRaw = rzRaw
        lastWorldRotRaw = worldRotRaw; lastWorldZoom = worldZoom; lastWorldTilt = worldTilt
        lastPosX = worldCenterX; lastPosY = worldCenterY; lastPosZ = worldCenterZ
        lastColorZMinus = colorZMinus; lastColorZPlus = colorZPlus
        lastColorYMinus = colorYMinus; lastColorXPlus = colorXPlus
        lastColorYPlus = colorYPlus; lastColorXMinus = colorXMinus

        val sqrtTwo = 1.4142135f
        val tileWidthMultiplier = worldZoom * sqrtTwo * 2f
        val tileHeightMultiplier = worldZoom * sqrtTwo * worldTilt
        val depthEffect = (tileHeightMultiplier / tileWidthMultiplier) * 1.5f

        val wCos = worldRotation.cos
        val wSin = worldRotation.sin
        val cameraCenterX = worldCenterX * wCos - worldCenterY * wSin
        val cameraCenterY = worldCenterX * wSin + worldCenterY * wCos
        val cameraCenterZ = worldCenterZ
        // The depth sort axis must match the projection's line of sight, whose kernel is
        // (1, 1, 0.75 * tilt) — see projX/projY below. A constant Z weight desyncs as tilt changes.
        drawingOrder = -(cameraCenterX + cameraCenterY + 0.75f * worldTilt * cameraCenterZ)
        if (sxRaw != lastSizeXRaw || syRaw != lastSizeYRaw || szRaw != lastSizeZRaw) {
            lastSizeXRaw = sxRaw
            lastSizeYRaw = syRaw
            lastSizeZRaw = szRaw
            val hx = sxRaw * 0.5f;
            val hy = syRaw * 0.5f;
            val hz = szRaw * 0.5f
            cubeX[0] = -hx; cubeY[0] = -hy; cubeZ[0] = -hz
            cubeX[1] = hx; cubeY[1] = -hy; cubeZ[1] = -hz
            cubeX[2] = hx; cubeY[2] = hy; cubeZ[2] = -hz
            cubeX[3] = -hx; cubeY[3] = hy; cubeZ[3] = -hz
            cubeX[4] = -hx; cubeY[4] = -hy; cubeZ[4] = hz
            cubeX[5] = hx; cubeY[5] = -hy; cubeZ[5] = hz
            cubeX[6] = hx; cubeY[6] = hy; cubeZ[6] = hz
            cubeX[7] = -hx; cubeY[7] = hy; cubeZ[7] = hz
        }
        val rxCos = renderableCuboid.rotationX.cos;
        val rxSin = renderableCuboid.rotationX.sin
        val ryCos = renderableCuboid.rotationY.cos;
        val rySin = renderableCuboid.rotationY.sin
        val totalRz = renderableCuboid.rotationZ + worldRotation
        val rzCos = totalRz.cos;
        val rzSin = totalRz.sin
        val m00 = rzCos * ryCos
        val m01 = rzCos * rxSin * rySin - rzSin * rxCos
        val m02 = rzCos * rxCos * rySin + rzSin * rxSin
        val m10 = rzSin * ryCos
        val m11 = rzSin * rxSin * rySin + rzCos * rxCos
        val m12 = rzSin * rxCos * rySin - rzCos * rxSin
        val m20 = -rySin
        val m21 = rxSin * ryCos
        val m22 = rxCos * ryCos
        for (i in 0 until 8) {
            val px = cubeX[i]
            val py = cubeY[i]
            val pz = cubeZ[i]
            val lx = px * m00 + py * m01 + pz * m02
            val ly = px * m10 + py * m11 + pz * m12
            val lz = px * m20 + py * m21 + pz * m22
            camX[i] = lx + cameraCenterX
            camY[i] = ly + cameraCenterY
            camZ[i] = lz + cameraCenterZ
            projX[i] = (lx - ly) * 0.5f * tileWidthMultiplier
            projY[i] = (lx + ly) * 0.5f * tileHeightMultiplier - (lz / depthEffect) * tileHeightMultiplier
        }
        var minX = projX[0];
        var maxX = projX[0]
        var minY = projY[0];
        var maxY = projY[0]
        for (i in 1 until 8) {
            val px = projX[i];
            val py = projY[i]
            if (px < minX) minX = px; if (px > maxX) maxX = px
            if (py < minY) minY = py; if (py > maxY) maxY = py
        }
        val strokePadding = STROKE_WIDTH
        minX -= strokePadding; maxX += strokePadding
        minY -= strokePadding; maxY += strokePadding
        body.size = SceneSize(((maxX - minX) + 1f).sceneUnit, ((maxY - minY) + 1f).sceneUnit)
        body.pivot = SceneOffset((-minX).sceneUnit, (-minY).sceneUnit)
        body.position = SceneOffset(
            x = ((cameraCenterX - cameraCenterY) * 0.5f * tileWidthMultiplier).sceneUnit,
            y = ((cameraCenterX + cameraCenterY) * 0.5f * tileHeightMultiplier - (cameraCenterZ / depthEffect) * tileHeightMultiplier).sceneUnit,
        )
        visibleCount = 0
        for (f in 0 until 6) {
            val idx = FACE_INDICES[f]
            val px0 = projX[idx[0]] - minX;
            val py0 = projY[idx[0]] - minY
            val px1 = projX[idx[1]] - minX;
            val py1 = projY[idx[1]] - minY
            val px2 = projX[idx[2]] - minX;
            val py2 = projY[idx[2]] - minY
            val px3 = projX[idx[3]] - minX;
            val py3 = projY[idx[3]] - minY
            val sum = (px1 - px0) * (py1 + py0) +
                    (px2 - px1) * (py2 + py1) +
                    (px3 - px2) * (py3 + py2) +
                    (px0 - px3) * (py0 + py3)
            if (sum > 0f) {
                val d = (camX[idx[0]] + camY[idx[0]] + camZ[idx[0]] +
                        camX[idx[1]] + camY[idx[1]] + camZ[idx[1]] +
                        camX[idx[2]] + camY[idx[2]] + camZ[idx[2]] +
                        camX[idx[3]] + camY[idx[3]] + camZ[idx[3]]) * 0.25f

                faceDepth[visibleCount] = d
                faceOrder[visibleCount] = f
                visibleFaceId[visibleCount] = f
                faceColors[visibleCount] = when (f) {
                    0 -> colorZMinus
                    1 -> colorZPlus
                    2 -> colorYMinus
                    3 -> colorXPlus
                    4 -> colorYPlus
                    else -> colorXMinus
                } ?: Color.White
                faceArgb[visibleCount] = faceColors[visibleCount].toArgb()

                texP0x[visibleCount] = px0; texP0y[visibleCount] = py0
                texP1x[visibleCount] = px1; texP1y[visibleCount] = py1
                texP2x[visibleCount] = px2; texP2y[visibleCount] = py2
                texP3x[visibleCount] = px3; texP3y[visibleCount] = py3

                visibleCount++
            }
        }
        for (i in 0 until visibleCount) {
            var best = i
            var bestDepth = faceDepth[i]
            for (j in i + 1 until visibleCount) {
                val dj = faceDepth[j]
                if (dj > bestDepth) {
                    bestDepth = dj
                    best = j
                }
            }
            if (best != i) {
                val td = faceDepth[i]; faceDepth[i] = faceDepth[best]; faceDepth[best] = td
                val to = faceOrder[i]; faceOrder[i] = faceOrder[best]; faceOrder[best] = to
                val tv = visibleFaceId[i]; visibleFaceId[i] = visibleFaceId[best]; visibleFaceId[best] = tv
                val tc = faceColors[i]; faceColors[i] = faceColors[best]; faceColors[best] = tc
                val ta = faceArgb[i]; faceArgb[i] = faceArgb[best]; faceArgb[best] = ta

                val t0x = texP0x[i]; texP0x[i] = texP0x[best]; texP0x[best] = t0x
                val t0y = texP0y[i]; texP0y[i] = texP0y[best]; texP0y[best] = t0y
                val t1x = texP1x[i]; texP1x[i] = texP1x[best]; texP1x[best] = t1x
                val t1y = texP1y[i]; texP1y[i] = texP1y[best]; texP1y[best] = t1y
                val t2x = texP2x[i]; texP2x[i] = texP2x[best]; texP2x[best] = t2x
                val t2y = texP2y[i]; texP2y[i] = texP2y[best]; texP2y[best] = t2y
                val t3x = texP3x[i]; texP3x[i] = texP3x[best]; texP3x[best] = t3x
                val t3y = texP3y[i]; texP3y[i] = texP3y[best]; texP3y[best] = t3y
            }
        }
        return true
    }

    private fun applyParallelogramTransform(
        p0x: Float, p0y: Float,
        p1x: Float, p1y: Float,
        p3x: Float, p3y: Float,
        baseW: Float, baseH: Float
    ) {
        val v = transformMatrix.values
        v[0] = (p1x - p0x) / baseW
        v[1] = (p1y - p0y) / baseW
        v[4] = (p3x - p0x) / baseH
        v[5] = (p3y - p0y) / baseH
        v[12] = p0x
        v[13] = p0y
        v[2] = 0f; v[3] = 0f
        v[6] = 0f; v[7] = 0f
        v[8] = 0f; v[9] = 0f; v[10] = 1f; v[11] = 0f
        v[14] = 0f; v[15] = 1f
    }

    // Texture names never change at runtime, so resolve each face once and cache its mip chain.
    // Faces with no texture stay null; unresolved faces retry until the bitmap loads. The returned
    // bitmap is the mip level matching the current zoom, so minified faces sample a smaller image.
    private fun resolveFaceTexture(faceId: Int): ImageBitmap? {
        resolvedTextures[faceId]?.let { return it.forZoom(currentZoom) }
        val name = when (faceId) {
            0 -> cuboid.textureZMinus
            1 -> cuboid.textureZPlus
            2 -> cuboid.textureYMinus
            3 -> cuboid.textureXPlus
            4 -> cuboid.textureYPlus
            else -> cuboid.textureXMinus
        } ?: return null
        return textureResolver(name)
            ?.let { TextureMipChain(it).also { chain -> resolvedTextures[faceId] = chain } }
            ?.forZoom(currentZoom)
    }

    // Batched drawing used by VolumetricCuboidBatchRenderer: solid faces and outlines become
    // triangles in [batch] (in painter's order), textured faces flush the batch and draw through
    // the canvas. Unlike [draw], the coordinates are absolute scene coordinates, because the whole
    // scene is emitted under a single canvas transform. [pixelsPerUnit] is only used for LOD
    // decisions; geometry stays in scene units.
    internal fun DrawScope.emitBatched(batch: TriangleBatch, pixelsPerUnit: Float) {
        if (visibleCount == 0) return
        val originX = body.position.x.raw - body.pivot.x.raw
        val originY = body.position.y.raw - body.pivot.y.raw
        val contentWidthPx = (body.size.width.raw - 2 * STROKE_WIDTH) * pixelsPerUnit
        val contentHeightPx = (body.size.height.raw - 2 * STROKE_WIDTH) * pixelsPerUnit
        if (contentWidthPx < STROKE_WIDTH && contentHeightPx < STROKE_WIDTH) return
        val alphaMultiplier = if (isTranslucent) 0.2f else 1f
        if (contentWidthPx < STROKE_WIDTH * 2 && contentHeightPx < STROKE_WIDTH * 2) {
            // Distant cuboids collapse to a single dark quad, mirroring PlanarCuboidRenderer's
            // dot fallback: at this size the outline would dominate the faces anyway.
            val centerX = originX + body.size.width.raw * 0.5f
            val centerY = originY + body.size.height.raw * 0.5f
            val radius = STROKE_WIDTH / pixelsPerUnit
            batch.addQuad(
                p0x = centerX - radius, p0y = centerY - radius,
                p1x = centerX + radius, p1y = centerY - radius,
                p2x = centerX + radius, p2y = centerY + radius,
                p3x = centerX - radius, p3y = centerY + radius,
                argb = applyAlphaMultiplier(OUTLINE_ARGB, alphaMultiplier),
            )
            return
        }
        for (i in 0 until visibleCount) {
            val texture = resolveFaceTexture(visibleFaceId[i])
            if (texture == null) {
                batch.addQuad(
                    p0x = texP0x[i] + originX, p0y = texP0y[i] + originY,
                    p1x = texP1x[i] + originX, p1y = texP1y[i] + originY,
                    p2x = texP2x[i] + originX, p2y = texP2y[i] + originY,
                    p3x = texP3x[i] + originX, p3y = texP3y[i] + originY,
                    argb = applyAlphaMultiplier(faceArgb[i], alphaMultiplier),
                )
            } else {
                val canvas = drawContext.canvas
                batch.flush(canvas)
                canvas.save()
                applyParallelogramTransform(
                    p0x = texP0x[i] + originX, p0y = texP0y[i] + originY,
                    p1x = texP1x[i] + originX, p1y = texP1y[i] + originY,
                    p3x = texP3x[i] + originX, p3y = texP3y[i] + originY,
                    baseW = texture.width.toFloat(), baseH = texture.height.toFloat(),
                )
                canvas.concat(transformMatrix)
                drawImage(image = texture, alpha = alphaMultiplier)
                canvas.restore()
            }
        }
        // Small cuboids skip the outline entirely: at that size it is sub-pixel clutter that used
        // to cost a full anti-aliased path stroke per cuboid. Edges shared by two visible faces
        // are deduplicated through a 12-bit cube-edge mask, so each is emitted (and filled) once.
        if (!isTranslucent && (contentWidthPx >= OUTLINE_MIN_SIZE_PX || contentHeightPx >= OUTLINE_MIN_SIZE_PX)) {
            val halfWidth = STROKE_WIDTH * 0.5f
            val centerX = body.position.x.raw
            val centerY = body.position.y.raw
            var edgeMask = 0
            for (i in 0 until visibleCount) {
                edgeMask = edgeMask or FACE_EDGE_MASKS[visibleFaceId[i]]
            }
            var edge = 0
            while (edgeMask != 0) {
                if (edgeMask and 1 != 0) {
                    val a = EDGE_CORNER_A[edge]
                    val b = EDGE_CORNER_B[edge]
                    batch.addLine(
                        ax = projX[a] + centerX, ay = projY[a] + centerY,
                        bx = projX[b] + centerX, by = projY[b] + centerY,
                        halfWidth = halfWidth,
                        argb = OUTLINE_ARGB,
                    )
                }
                edgeMask = edgeMask ushr 1
                edge++
            }
        }
    }

    override fun DrawScope.draw() {
        val alphaMultiplier = if (isTranslucent) 0.2f else 1f
        val canvas = drawContext.canvas

        for (i in 0 until visibleCount) {
            val texture = resolveFaceTexture(visibleFaceId[i])

            val p0x = texP0x[i];
            val p0y = texP0y[i]
            val p1x = texP1x[i];
            val p1y = texP1y[i]
            val p3x = texP3x[i];
            val p3y = texP3y[i]

            canvas.save()
            if (texture != null) {
                applyParallelogramTransform(
                    p0x, p0y, p1x, p1y, p3x, p3y,
                    texture.width.toFloat(), texture.height.toFloat()
                )
                canvas.concat(transformMatrix)
                drawImage(image = texture, alpha = alphaMultiplier)
            } else {
                applyParallelogramTransform(p0x, p0y, p1x, p1y, p3x, p3y, 1f, 1f)
                canvas.concat(transformMatrix)
                drawRect(color = faceColors[i], size = Size(1f, 1f), alpha = alphaMultiplier)
            }
            canvas.restore()
        }

        if (!isTranslucent && visibleCount > 0) {
            outlinePath.rewind()
            for (i in 0 until visibleCount) {
                outlinePath.moveTo(texP0x[i], texP0y[i])
                outlinePath.lineTo(texP1x[i], texP1y[i])
                outlinePath.lineTo(texP2x[i], texP2y[i])
                outlinePath.lineTo(texP3x[i], texP3y[i])
                outlinePath.close()
            }
            drawPath(outlinePath, Color.Black, style = outlineStroke)
        }
    }

    companion object {
        const val STROKE_WIDTH = 4f
        private const val OUTLINE_MIN_SIZE_PX = 12f
        private const val OUTLINE_ARGB = 0xFF000000.toInt()

        private fun applyAlphaMultiplier(argb: Int, multiplier: Float): Int =
            if (multiplier >= 1f) argb
            else (((argb ushr 24).toFloat() * multiplier).toInt().coerceIn(0, 255) shl 24) or (argb and 0x00FFFFFF)

        private val FACE_INDICES: Array<IntArray> = arrayOf(
            intArrayOf(0, 1, 2, 3), // 0: Z-
            intArrayOf(4, 7, 6, 5), // 1: Z+
            intArrayOf(0, 4, 5, 1), // 2: Y-
            intArrayOf(1, 5, 6, 2), // 3: X+
            intArrayOf(2, 6, 7, 3), // 4: Y+
            intArrayOf(3, 7, 4, 0), // 5: X-
        )

        // The 12 cube edges as corner pairs: 0-3 bottom ring, 4-7 top ring, 8-11 verticals.
        private val EDGE_CORNER_A = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3)
        private val EDGE_CORNER_B = intArrayOf(1, 2, 3, 0, 5, 6, 7, 4, 4, 5, 6, 7)

        // Per face (same order as FACE_INDICES), the bitmask of the cube edges on its border.
        private val FACE_EDGE_MASKS = intArrayOf(
            (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3), // Z-: bottom ring
            (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7), // Z+: top ring
            (1 shl 8) or (1 shl 4) or (1 shl 9) or (1 shl 0), // Y-
            (1 shl 9) or (1 shl 5) or (1 shl 10) or (1 shl 1), // X+
            (1 shl 10) or (1 shl 6) or (1 shl 11) or (1 shl 2), // Y+
            (1 shl 11) or (1 shl 7) or (1 shl 8) or (1 shl 3), // X-
        )
    }
}