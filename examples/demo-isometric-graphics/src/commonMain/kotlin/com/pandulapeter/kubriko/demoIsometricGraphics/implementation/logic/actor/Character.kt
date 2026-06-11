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

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.helpers.extensions.rotateTowards
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidModelRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    miniMapMarker = MiniMapMarker.Circle,
    isPreferredMiniMapMarker = { it == "Head" },
    isDrawn = false,
) {
    override fun update(deltaTimeInMilliseconds: Int) {
        renderableCuboidModel.rotationZ = renderableCuboidModel.rotationZ.rotateTowards(
            target = position.positionXY.angleTowards(mainCharacter.renderableCuboidModel.position.positionXY),
            maxDelta = AngleRadians.HalfPi * 0.001f * deltaTimeInMilliseconds,
        )
        super.update(deltaTimeInMilliseconds)
    }

    companion object {
        const val ID_PREFIX = "character"
    }
}