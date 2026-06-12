/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.actor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidModelRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.helpers.extensions.rotateTowards
import com.pandulapeter.kubriko.types.AngleRadians
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val CHARACTER_BODY_PALETTE = listOf(
    Triple(0.20f, 0.40f, 0.70f),
    Triple(0.70f, 0.20f, 0.20f),
    Triple(0.20f, 0.55f, 0.30f),
    Triple(0.55f, 0.28f, 0.65f),
    Triple(0.70f, 0.48f, 0.18f),
)

@OptIn(ExperimentalUuidApi::class)
class Character(
    private val mainCharacter: MainCharacter,
    cuboidModel: CuboidModel,
    private val position: Vec3,
    textureResolver: (String) -> ImageBitmap?,
) : PlanarCuboidModelRenderer(
    renderableCuboidModel = RenderableCuboidModel(
        id = "${ID_PREFIX}_${Uuid.random()}",
        cuboidModel = cuboidModel,
        position = position,
        rotationZ = AngleRadians.Zero,
    ),
    projectionPlane = ProjectionPlane.XY,
    textureResolver = textureResolver,
    miniMapMarker = CircleMiniMapMarker,
    isPreferredMiniMapMarker = { it == "Head" },
    isDrawn = false,
) {
    init {
        val base = CHARACTER_BODY_PALETTE[Random.nextInt(CHARACTER_BODY_PALETTE.size)]
        val variation = Random.nextFloat() * 0.06f
        val bodyColor = Color(
            red = (base.first + variation).coerceAtMost(1f),
            green = (base.second + variation).coerceAtMost(1f),
            blue = (base.third + variation).coerceAtMost(1f),
            alpha = 1f,
        )
        val feetColor = Color(
            red = bodyColor.red * 0.5f,
            green = bodyColor.green * 0.5f,
            blue = bodyColor.blue * 0.5f,
            alpha = 1f,
        )
        renderableCuboidModel.cuboidModel.cuboids.values.forEach { cuboid ->
            val color = if (cuboid.name == "Left Foot" || cuboid.name == "Right Foot") feetColor else bodyColor
            cuboid.colorXPlus = color
            cuboid.colorXMinus = color
            cuboid.colorYPlus = color
            cuboid.colorYMinus = color
            cuboid.colorZPlus = color
            cuboid.colorZMinus = color
        }
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        renderableCuboidModel.rotationZ = renderableCuboidModel.rotationZ.rotateTowards(
            target = position.positionXY.angleTowards(mainCharacter.renderableCuboidModel.position.positionXY),
            maxDelta = AngleRadians.HalfPi * 0.001f * deltaTimeInMilliseconds,
        )
        super.update(deltaTimeInMilliseconds)
    }

    companion object {
        const val ID_PREFIX = "character"

        private object CircleMiniMapMarker : MiniMapMarker {
            override fun DrawScope.draw(
                centerX: Float,
                centerY: Float,
                halfWidth: Float,
                halfHeight: Float,
                rotation: Float,
                color: Color,
                stroke: Stroke,
            ) {
                val radius = halfWidth * 2
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(centerX, centerY),
                )
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = stroke,
                )
            }
        }
    }
}
