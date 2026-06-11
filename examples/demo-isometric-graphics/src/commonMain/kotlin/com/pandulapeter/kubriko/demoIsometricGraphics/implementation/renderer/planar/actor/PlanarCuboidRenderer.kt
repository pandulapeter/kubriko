/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.Cuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboid
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.RenderableCuboidHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane

open class PlanarCuboidRenderer(
    modelId: String,
    val cuboidId: String,
    private val cuboid: Cuboid,
    private val projectionPlane: ProjectionPlane,
    override val textureResolver: (String) -> ImageBitmap?,
    initialModelPositionX: SceneUnit = SceneUnit.Zero,
    initialModelPositionY: SceneUnit = SceneUnit.Zero,
    initialModelPositionZ: SceneUnit = SceneUnit.Zero,
    initialModelRotationZ: AngleRadians = AngleRadians.Zero,
    override val miniMapMarker: MiniMapMarker? = null,
    private val isPreferredMiniMapMarker: (String) -> Boolean = { true },
    isDrawn: Boolean = true,
) : Visible, RenderableCuboidHolder {

    // The game never shows the planar projection (its viewport exists only to drive culling), so
    // its actors opt out of drawing entirely: the engine skips recording their draw operations
    // while viewport culling — which ignores isVisible — still feeds them to the 3D pipeline.
    final override val isVisible = isDrawn

    override fun isPreferredMiniMapMarker(cuboidName: String) = isPreferredMiniMapMarker.invoke(cuboidName)

    final override val id = "$modelId-$cuboidId"

    override val renderableCuboid: RenderableCuboid = RenderableCuboid(
        cuboid = cuboid,
        positionInWorld = Vec3.Zero,
    )

    final override val body = BoxBody()
    final override var drawingOrder = 0f

    private var modelPosX = 0f
    private var modelPosY = 0f
    private var modelPosZ = 0f
    private var modelRotZ: AngleRadians = AngleRadians.Zero

    private var boundsMinXRaw = 0f
    private var boundsMinYRaw = 0f

    private val cubeX = FloatArray(8)
    private val cubeY = FloatArray(8)
    private val cubeZ = FloatArray(8)

    private val projX = FloatArray(8)
    private val projY = FloatArray(8)
    protected var projectedSceneOffsets: List<SceneOffset> = object : AbstractList<SceneOffset>() {
        override val size: Int get() = 8
        override fun get(index: Int): SceneOffset = SceneOffset(projX[index].sceneUnit, projY[index].sceneUnit)
    }

    private val faceColors: Array<Color> = Array(6) { Color.Transparent }
    private val visibleFaceId: IntArray = IntArray(6)
    private var visibleCount = 0

    private val texP0x = FloatArray(6);
    private val texP0y = FloatArray(6)
    private val texP1x = FloatArray(6);
    private val texP1y = FloatArray(6)
    private val texP2x = FloatArray(6);
    private val texP2y = FloatArray(6)
    private val texP3x = FloatArray(6);
    private val texP3y = FloatArray(6)

    private val transformMatrix = Matrix()

    private val resolvedTextures = arrayOfNulls<ImageBitmap>(6)
    private val outlinePath = Path()
    private var outlineStroke = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Round)
    private var lastStrokeWidth = STROKE_WIDTH

    private var lastSizeXRaw = Float.NaN
    private var lastSizeYRaw = Float.NaN
    private var lastSizeZRaw = Float.NaN
    private var lastModelPosXRaw = Float.NaN
    private var lastModelPosYRaw = Float.NaN
    private var lastModelPosZRaw = Float.NaN
    private var lastModelRotZRaw = Float.NaN
    private var lastLocalXRaw = Float.NaN
    private var lastLocalYRaw = Float.NaN
    private var lastLocalZRaw = Float.NaN
    private var lastRxRaw = Float.NaN
    private var lastRyRaw = Float.NaN
    private var lastRzRaw = Float.NaN
    private var lastColorXPlus: Color? = null
    private var lastColorXMinus: Color? = null
    private var lastColorYPlus: Color? = null
    private var lastColorYMinus: Color? = null
    private var lastColorZPlus: Color? = null
    private var lastColorZMinus: Color? = null
    protected lateinit var viewportManager: ViewportManager

    init {
        update(
            modelPositionX = initialModelPositionX,
            modelPositionY = initialModelPositionY,
            modelPositionZ = initialModelPositionZ,
            modelRotationZ = initialModelRotationZ,
        )
    }

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }

    fun update(
        modelPositionX: SceneUnit = SceneUnit.Zero,
        modelPositionY: SceneUnit = SceneUnit.Zero,
        modelPositionZ: SceneUnit = SceneUnit.Zero,
        modelRotationZ: AngleRadians = AngleRadians.Zero,
    ) {
        val modelPosXRaw = modelPositionX.raw
        val modelPosYRaw = modelPositionY.raw
        val modelPosZRaw = modelPositionZ.raw
        val modelRotZRaw = modelRotationZ.raw
        val localX = cuboid.positionX.raw
        val localY = cuboid.positionY.raw
        val localZ = cuboid.positionZ.raw
        val sx = cuboid.sizeX.raw
        val sy = cuboid.sizeY.raw
        val sz = cuboid.sizeZ.raw
        val rxRaw = cuboid.rotationX.raw
        val ryRaw = cuboid.rotationY.raw
        val rzRaw = cuboid.rotationZ.raw
        val colorXPlus = cuboid.colorXPlus
        val colorXMinus = cuboid.colorXMinus
        val colorYPlus = cuboid.colorYPlus
        val colorYMinus = cuboid.colorYMinus
        val colorZPlus = cuboid.colorZPlus
        val colorZMinus = cuboid.colorZMinus

        if (modelPosXRaw == lastModelPosXRaw && modelPosYRaw == lastModelPosYRaw && modelPosZRaw == lastModelPosZRaw
            && modelRotZRaw == lastModelRotZRaw
            && localX == lastLocalXRaw && localY == lastLocalYRaw && localZ == lastLocalZRaw
            && sx == lastSizeXRaw && sy == lastSizeYRaw && sz == lastSizeZRaw
            && rxRaw == lastRxRaw && ryRaw == lastRyRaw && rzRaw == lastRzRaw
            && colorXPlus == lastColorXPlus && colorXMinus == lastColorXMinus
            && colorYPlus == lastColorYPlus && colorYMinus == lastColorYMinus
            && colorZPlus == lastColorZPlus && colorZMinus == lastColorZMinus) {
            return
        }

        lastModelPosXRaw = modelPosXRaw; lastModelPosYRaw = modelPosYRaw; lastModelPosZRaw = modelPosZRaw
        lastModelRotZRaw = modelRotZRaw
        lastLocalXRaw = localX; lastLocalYRaw = localY; lastLocalZRaw = localZ
        lastRxRaw = rxRaw; lastRyRaw = ryRaw; lastRzRaw = rzRaw
        lastColorXPlus = colorXPlus; lastColorXMinus = colorXMinus
        lastColorYPlus = colorYPlus; lastColorYMinus = colorYMinus
        lastColorZPlus = colorZPlus; lastColorZMinus = colorZMinus

        this.modelPosX = modelPosXRaw
        this.modelPosY = modelPosYRaw
        this.modelPosZ = modelPosZRaw
        this.modelRotZ = modelRotationZ

        val mCos = modelRotationZ.cos
        val mSin = modelRotationZ.sin

        // 1. Calculate precise World Center (Preserves exact orbital math)
        val worldX = localX * mCos - localY * mSin + modelPosX
        val worldY = localX * mSin + localY * mCos + modelPosY
        val worldZ = localZ + modelPosZ

        renderableCuboid.positionInWorld.apply {
            x = worldX.sceneUnit
            y = worldY.sceneUnit
            z = worldZ.sceneUnit
        }
        renderableCuboid.rotationX = cuboid.rotationX
        renderableCuboid.rotationY = cuboid.rotationY
        renderableCuboid.rotationZ = cuboid.rotationZ + modelRotZ

        drawingOrder = when (projectionPlane) {
            ProjectionPlane.XY -> -(localZ + modelPosZ) - cuboid.sizeZ.raw * 0.5f
            ProjectionPlane.XZ -> -(localY + modelPosY) - cuboid.sizeY.raw * 0.5f
            ProjectionPlane.YZ -> -(localX + modelPosX) - cuboid.sizeX.raw * 0.5f
        }

        // 2. Base mesh caching
        if (sx != lastSizeXRaw || sy != lastSizeYRaw || sz != lastSizeZRaw) {
            lastSizeXRaw = sx; lastSizeYRaw = sy; lastSizeZRaw = sz
            val hx = sx * 0.5f;
            val hy = sy * 0.5f;
            val hz = sz * 0.5f
            cubeX[0] = -hx; cubeY[0] = -hy; cubeZ[0] = -hz
            cubeX[1] = hx; cubeY[1] = -hy; cubeZ[1] = -hz
            cubeX[2] = hx; cubeY[2] = hy; cubeZ[2] = -hz
            cubeX[3] = -hx; cubeY[3] = hy; cubeZ[3] = -hz
            cubeX[4] = -hx; cubeY[4] = -hy; cubeZ[4] = hz
            cubeX[5] = hx; cubeY[5] = -hy; cubeZ[5] = hz
            cubeX[6] = hx; cubeY[6] = hy; cubeZ[6] = hz
            cubeX[7] = -hx; cubeY[7] = hy; cubeZ[7] = hz
        }
        val cx = cuboid.rotationX.cos;
        val sxR = cuboid.rotationX.sin
        val cy = cuboid.rotationY.cos;
        val syR = cuboid.rotationY.sin
        val totalRz = cuboid.rotationZ + modelRotZ
        val cz = totalRz.cos;
        val szR = totalRz.sin
        val m00 = cz * cy
        val m01 = cz * sxR * syR - szR * cx
        val m02 = cz * cx * syR + szR * sxR
        val m10 = szR * cy
        val m11 = szR * sxR * syR + cz * cx
        val m12 = szR * cx * syR - cz * sxR
        val m20 = -syR
        val m21 = sxR * cy
        val m22 = cx * cy
        for (i in 0 until 8) {
            val lx = cubeX[i];
            val ly = cubeY[i];
            val lz = cubeZ[i]
            val wx = lx * m00 + ly * m01 + lz * m02 + worldX
            val wy = lx * m10 + ly * m11 + lz * m12 + worldY
            val wz = lx * m20 + ly * m21 + lz * m22 + worldZ
            when (projectionPlane) {
                ProjectionPlane.XY -> {
                    projX[i] = wx
                    projY[i] = wy
                }

                ProjectionPlane.XZ -> {
                    projX[i] = wx
                    projY[i] = -wz
                }

                ProjectionPlane.YZ -> {
                    projX[i] = -wy
                    projY[i] = -wz
                }
            }
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
        minX -= STROKE_WIDTH; maxX += STROKE_WIDTH
        minY -= STROKE_WIDTH; maxY += STROKE_WIDTH
        boundsMinXRaw = minX; boundsMinYRaw = minY
        val sizeW = maxX - minX;
        val sizeH = maxY - minY
        body.size = SceneSize(sizeW.sceneUnit, sizeH.sceneUnit)
        body.pivot = body.size.center
        body.position = SceneOffset(
            x = (minX + body.pivot.x.raw).sceneUnit,
            y = (minY + body.pivot.y.raw).sceneUnit,
        )
        val vX: Float
        val vY: Float
        val vZ: Float
        when (projectionPlane) {
            ProjectionPlane.XY -> {
                vX = m20; vY = m21; vZ = m22
            }

            ProjectionPlane.XZ -> {
                vX = m10; vY = m11; vZ = m12
            }

            ProjectionPlane.YZ -> {
                vX = m00; vY = m01; vZ = m02
            }
        }

        visibleCount = 0

        fun recordFace(f: Int, color: Color?) {
            visibleFaceId[visibleCount] = f
            val idx = FACE_INDICES[f]
            texP0x[visibleCount] = projX[idx[0]] - boundsMinXRaw; texP0y[visibleCount] = projY[idx[0]] - boundsMinYRaw
            texP1x[visibleCount] = projX[idx[1]] - boundsMinXRaw; texP1y[visibleCount] = projY[idx[1]] - boundsMinYRaw
            texP2x[visibleCount] = projX[idx[2]] - boundsMinXRaw; texP2y[visibleCount] = projY[idx[2]] - boundsMinYRaw
            texP3x[visibleCount] = projX[idx[3]] - boundsMinXRaw; texP3y[visibleCount] = projY[idx[3]] - boundsMinYRaw
            faceColors[visibleCount] = color ?: Color.White
            visibleCount++
        }

        if (vX > 0f) recordFace(0, cuboid.colorXPlus)
        if (vX < 0f) recordFace(1, cuboid.colorXMinus)
        if (vY > 0f) recordFace(2, cuboid.colorYPlus)
        if (vY < 0f) recordFace(3, cuboid.colorYMinus)
        if (vZ > 0f) recordFace(4, cuboid.colorZPlus)
        if (vZ < 0f) recordFace(5, cuboid.colorZMinus)

        onUpdated()
    }

    open fun onUpdated() = Unit
    open val isTranslucent = false

    private fun applyParallelogramTransform(
        p0x: Float, p0y: Float, p1x: Float, p1y: Float, p3x: Float, p3y: Float, baseW: Float, baseH: Float
    ) {
        val v = transformMatrix.values
        v[0] = (p1x - p0x) / baseW; v[1] = (p1y - p0y) / baseW
        v[4] = (p3x - p0x) / baseH; v[5] = (p3y - p0y) / baseH
        v[12] = p0x; v[13] = p0y
        v[2] = 0f; v[3] = 0f; v[6] = 0f; v[7] = 0f
        v[8] = 0f; v[9] = 0f; v[10] = 1f; v[11] = 0f
        v[14] = 0f; v[15] = 1f
    }

    // Texture names never change at runtime, so resolve each face once and cache it.
    // Faces with no texture stay null; unresolved faces retry until the bitmap loads.
    private fun resolveFaceTexture(faceId: Int): ImageBitmap? {
        resolvedTextures[faceId]?.let { return it }
        val name = when (faceId) {
            0 -> cuboid.textureXPlus
            1 -> cuboid.textureXMinus
            2 -> cuboid.textureYPlus
            3 -> cuboid.textureYMinus
            4 -> cuboid.textureZPlus
            else -> cuboid.textureZMinus
        } ?: return null
        return textureResolver(name)?.also { resolvedTextures[faceId] = it }
    }

    override fun DrawScope.draw() {
        val scaleFactor = viewportManager.scaleFactor.value.horizontal
        val contentWidthPx = (body.size.width.raw - 2 * STROKE_WIDTH) * scaleFactor
        val contentHeightPx = (body.size.height.raw - 2 * STROKE_WIDTH) * scaleFactor
        if (contentWidthPx < STROKE_WIDTH && contentHeightPx < STROKE_WIDTH) return
        val alphaMultiplier = if (isTranslucent) 0.2f else 1f
        if (contentWidthPx < STROKE_WIDTH * 2 && contentHeightPx < STROKE_WIDTH * 2) {
            if (visibleCount > 0) {
                drawCircle(
                    color = Color.Black,
                    radius = STROKE_WIDTH / scaleFactor,
                    center = Offset(body.size.width.raw / 2, body.size.height.raw / 2),
                    alpha = alphaMultiplier,
                )
            }
            return
        }
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
                applyParallelogramTransform(p0x, p0y, p1x, p1y, p3x, p3y, texture.width.toFloat(), texture.height.toFloat())
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
            val strokeWidth = STROKE_WIDTH / scaleFactor
            if (strokeWidth != lastStrokeWidth) {
                lastStrokeWidth = strokeWidth
                outlineStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            }
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
        private val FACE_INDICES: Array<IntArray> = arrayOf(
            intArrayOf(1, 2, 6, 5), intArrayOf(0, 4, 7, 3), // X+, X-
            intArrayOf(2, 3, 7, 6), intArrayOf(0, 1, 5, 4), // Y+, Y-
            intArrayOf(4, 5, 6, 7), intArrayOf(0, 3, 2, 1), // Z+, Z-
        )
    }
}