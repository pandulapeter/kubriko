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
        val heightFactor = Random.nextFloat() * 0.4f + 0.8f // 0.8 to 1.2
        val foliageSizeFactor = Random.nextFloat() * 0.4f + 0.8f // 0.8 to 1.2
        val foliageColor = Color(
            red = Random.nextFloat() * 0.15f + 0.05f,
            green = Random.nextFloat() * 0.4f + 0.4f,
            blue = Random.nextFloat() * 0.15f + 0.05f,
            alpha = 1.0f
        )

        renderableCuboidModel.rotationZ = AngleRadians.TwoPi * Random.nextFloat()
        renderableCuboidModel.cuboidModel.cuboids.values.forEach { cuboid ->
            cuboid.positionZ = (cuboid.positionZ.raw * heightFactor).sceneUnit
            cuboid.sizeZ = (cuboid.sizeZ.raw * heightFactor).sceneUnit
            if (cuboid.name == "Foliage") {
                cuboid.sizeX = (cuboid.sizeX.raw * foliageSizeFactor).sceneUnit
                cuboid.sizeY = (cuboid.sizeY.raw * foliageSizeFactor).sceneUnit
                cuboid.colorXPlus = foliageColor
                cuboid.colorXMinus = foliageColor
                cuboid.colorYPlus = foliageColor
                cuboid.colorYMinus = foliageColor
                cuboid.colorZPlus = foliageColor
                cuboid.colorZMinus = foliageColor
            }
        }
    }
}
