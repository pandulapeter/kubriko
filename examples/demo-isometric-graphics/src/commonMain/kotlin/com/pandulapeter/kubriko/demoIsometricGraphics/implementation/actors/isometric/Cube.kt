/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.isometric

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.traits.IsometricRepresentation
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.GridManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.IsometricGraphicsDemoManager
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.sprite_texture_side
import kubriko.examples.demo_isometric_graphics.generated.resources.sprite_texture_top

internal class Cube(
    private val color: Color,
    private val shouldDrawShadow: Boolean,
    private val shouldUseTextures: Boolean,
    positionX: SceneUnit = SceneUnit.Zero,
    positionY: SceneUnit = SceneUnit.Zero,
    positionZ: SceneUnit = SceneUnit.Zero,
    dimensionX: SceneUnit = SceneUnit.Zero,
    dimensionY: SceneUnit = SceneUnit.Zero,
    dimensionZ: SceneUnit = SceneUnit.Zero,
    rotationZ: AngleRadians = AngleRadians.Zero,
    override val extraDepth: Float = 0f,
) : IsometricRepresentation(
    positionX = positionX,
    positionY = positionY,
    positionZ = positionZ,
    dimensionX = dimensionX,
    dimensionY = dimensionY,
    dimensionZ = dimensionZ,
    rotationZ = rotationZ,
) {
    private lateinit var isometricGraphicsDemoManager: IsometricGraphicsDemoManager
    private lateinit var gridManager: GridManager
    private lateinit var spriteManager: SpriteManager
    override val tileWidthMultiplier get() = gridManager.tileWidthMultiplier.value
    override val tileHeightMultiplier get() = gridManager.tileHeightMultiplier.value
    private val stroke = Stroke(
        width = 4f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    private var bottomFace = emptyList<Offset>()
    private var topFace = emptyList<Offset>()
    private var visibleFaces = emptyList<List<Offset>>()

    private fun Offset.rotateAround(center: Offset, radians: AngleRadians): Offset {
        val cosA = radians.cos
        val sinA = radians.sin
        val dx = x - center.x
        val dy = y - center.y
        return Offset(
            x = center.x + dx * cosA - dy * sinA,
            y = center.y + dx * sinA + dy * cosA
        )
    }

    private fun createSideFace(
        baseStart: Offset,
        baseEnd: Offset,
        topEnd: Offset,
        topStart: Offset
    ) = listOf(
        Offset(baseStart.x, baseStart.y),
        Offset(baseEnd.x, baseEnd.y),
        Offset(topEnd.x, topEnd.y),
        Offset(topStart.x, topStart.y),
    )

    private fun iso(v: Offset) = Offset((v.x - v.y) * 0.5f * tileWidthMultiplier, (v.x + v.y) * 0.5f * tileHeightMultiplier)

    override fun updateIsometricBody() {
        super.updateIsometricBody()
        val halfDimensionX = dimensionX.raw / 2f
        val halfDimensionY = dimensionY.raw / 2f
        val baseProjected = listOf(
            Offset(-halfDimensionX, -halfDimensionY),
            Offset(halfDimensionX, -halfDimensionY),
            Offset(halfDimensionX, halfDimensionY),
            Offset(-halfDimensionX, halfDimensionY)
        ).map { iso(it.rotateAround(Offset.Zero, rotationZ + AngleRadians.HalfPi)) + body.pivot.raw }
        bottomFace = baseProjected.map { Offset(it.x, it.y + positionZ.raw * tileHeightMultiplier) }
        topFace = baseProjected.map { Offset(it.x, it.y - dimensionZ.raw * tileHeightMultiplier) }
        visibleFaces = getVisibleFaces(
            baseProjected = baseProjected,
            facePaths = listOf(
                createSideFace(baseProjected[0], baseProjected[1], topFace[1], topFace[0]),
                createSideFace(baseProjected[1], baseProjected[2], topFace[2], topFace[1]),
                createSideFace(baseProjected[2], baseProjected[3], topFace[3], topFace[2]),
                createSideFace(baseProjected[3], baseProjected[0], topFace[0], topFace[3])
            ),
        )
    }

    private fun getVisibleFaces(
        baseProjected: List<Offset>,
        facePaths: List<List<Offset>>,
    ): List<List<Offset>> {
        val visibleFaces = mutableListOf<List<Offset>>()
        for (i in 0 until 4) {
            val nextI = (i + 1) % 4
            val edge = baseProjected[nextI] - baseProjected[i]
            val normal = Offset(-edge.y, edge.x)
            if (normal.y < -0.1f) visibleFaces.add(facePaths[i])
        }
        return visibleFaces
    }

    private fun List<Offset>.toPath() = Path().apply {
        if (this@toPath.isNotEmpty()) {
            moveTo(first().x, first().y)
            for (i in 1 until size) lineTo(this@toPath[i].x, this@toPath[i].y)
            close()
        }
    }

    override fun onAdded(kubriko: Kubriko) {
        isometricGraphicsDemoManager = kubriko.get()
        gridManager = kubriko.get()
        spriteManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        if (isometricGraphicsDemoManager.shouldDrawDebugBounds.value) {
            drawRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset.Zero,
                size = size,
            )
        }
        if (positionZ > SceneUnit.Zero && shouldDrawShadow) {
            drawPath(bottomFace.toPath(), Color.Black.copy(alpha = 0.2f))
        }
        val visibleFacePaths = visibleFaces.map { it.toPath() }
        visibleFacePaths.forEach { drawPath(it, color) }
        val topFacePath = topFace.toPath()
        drawPath(topFacePath, color)
        if (shouldUseTextures) {
            val textureTop = spriteManager.get(Res.drawable.sprite_texture_top)
            if (textureTop != null) {
                drawImageInParallelogram(
                    image = textureTop,
                    offsets = topFace,
                )
            }
            val textureSide = spriteManager.get(Res.drawable.sprite_texture_side)
            if (textureSide != null) {
                visibleFaces.forEach {
                    drawImageInParallelogram(
                        image = textureSide,
                        offsets = it,
                    )
                }
            }
        }
        visibleFacePaths.forEach { drawPath(it, Color.Black, style = stroke) }
        drawPath(topFacePath, Color.Black, style = stroke)
        if (isometricGraphicsDemoManager.shouldDrawDebugBounds.value) {
            drawCircle(
                color = Color.Black,
                radius = 10f,
                center = body.pivot.raw,
            )
        }
    }

    fun DrawScope.drawImageInParallelogram(
        image: ImageBitmap,
        offsets: List<Offset>,
    ) {
        if (offsets.size >= 4) {
            val p0 = offsets[0]
            val p1 = offsets[1]
            val p3 = offsets[3]
            val iw = image.width.toFloat()
            val ih = image.height.toFloat()
            val tx = p0.x
            val ty = p0.y
            val a = (p1.x - p0.x) / iw
            val c = (p1.y - p0.y) / iw
            val b = -(p3.x - p0.x) / ih
            val d = -(p3.y - p0.y) / ih
            val txAdjusted = tx + (p3.x - p0.x)
            val tyAdjusted = ty + (p3.y - p0.y)
            val p2Calc = p1 + (p3 - p0)
            clipPath(
                path = Path().apply {
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2Calc.x, p2Calc.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
            ) {
                withTransform(
                    transformBlock = {
                        transform(Matrix().apply {
                            this[0, 0] = a
                            this[1, 0] = b
                            this[0, 1] = c
                            this[1, 1] = d
                            this[3, 0] = txAdjusted
                            this[3, 1] = tyAdjusted
                        })
                    }
                ) {
                    drawImage(image, blendMode = BlendMode.Hardlight)
                }
            }
        }
    }
}