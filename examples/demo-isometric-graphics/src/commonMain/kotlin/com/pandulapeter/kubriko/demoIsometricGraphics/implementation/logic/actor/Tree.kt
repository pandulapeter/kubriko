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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidModelRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val TREE_FOLIAGE_PALETTE = listOf(
    Triple(0.10f, 0.22f, 0.09f),
    Triple(0.09f, 0.25f, 0.11f),
    Triple(0.11f, 0.21f, 0.10f),
    Triple(0.13f, 0.26f, 0.09f),
    Triple(0.16f, 0.28f, 0.09f),
)

@OptIn(ExperimentalUuidApi::class)
class Tree(
    cuboidModel: CuboidModel,
    position: Vec3,
    textureResolver: (String) -> ImageBitmap?,
) : PlanarCuboidModelRenderer(
    renderableCuboidModel = RenderableCuboidModel(
        id = "tree_${Uuid.random()}",
        cuboidModel = cuboidModel,
        position = position,
        rotationZ = AngleRadians.Zero,
    ),
    projectionPlane = ProjectionPlane.XY,
    textureResolver = textureResolver,
    miniMapMarker = MiniMapMarker.Rectangle,
    isPreferredMiniMapMarker = { it == "Foliage" },
    isDrawn = false,
    isStatic = true,
) {
    init {
        val heightFactor = Random.nextFloat() * 0.4f + 0.8f
        val foliageSizeFactor = Random.nextFloat() * 0.4f + 0.8f
        val base = TREE_FOLIAGE_PALETTE[Random.nextInt(TREE_FOLIAGE_PALETTE.size)]
        val variation = Random.nextFloat() * 0.05f
        val foliageColor = Color(
            red = base.first + variation,
            green = base.second + variation,
            blue = base.third + variation * 0.5f,
            alpha = 1f,
        )
        val foliageTopColor = Color(
            red = (foliageColor.red * 1.30f).coerceAtMost(1f),
            green = (foliageColor.green * 1.30f).coerceAtMost(1f),
            blue = (foliageColor.blue * 1.30f).coerceAtMost(1f),
            alpha = 1f,
        )

        renderableCuboidModel.rotationZ = AngleRadians.TwoPi * Random.nextFloat()
        renderableCuboidModel.cuboidModel.cuboids.values.forEach { cuboid ->
            cuboid.positionZ = (cuboid.positionZ.raw * heightFactor).sceneUnit
            cuboid.sizeZ = (cuboid.sizeZ.raw * heightFactor).sceneUnit
            if (cuboid.name == "Foliage") {
                cuboid.sizeX = (cuboid.sizeX.raw * foliageSizeFactor).sceneUnit
                cuboid.sizeY = (cuboid.sizeY.raw * foliageSizeFactor).sceneUnit
                cuboid.colorZPlus = foliageTopColor
                cuboid.colorZMinus = foliageColor
                cuboid.colorXPlus = foliageColor
                cuboid.colorXMinus = foliageColor
                cuboid.colorYPlus = foliageColor
                cuboid.colorYMinus = foliageColor
            }
        }
    }
}
