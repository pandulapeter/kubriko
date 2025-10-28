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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.actors.traits.IsometricRepresentation
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.GridManager
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.roundToInt

internal class Animal(
    furColor: Color,
    eyeColor: Color,
    positionX: SceneUnit = SceneUnit.Zero,
    positionY: SceneUnit = SceneUnit.Zero,
    positionZ: SceneUnit = SceneUnit.Zero,
    dimensionX: SceneUnit = SceneUnit.Zero,
    dimensionY: SceneUnit = SceneUnit.Zero,
    dimensionZ: SceneUnit = SceneUnit.Zero,
    rotationZ: AngleRadians = AngleRadians.Zero,
) : IsometricRepresentation(
    positionX = positionX,
    positionY = positionY,
    positionZ = positionZ,
    dimensionX = dimensionX,
    dimensionY = dimensionY,
    dimensionZ = dimensionZ,
    rotationZ = rotationZ,
), Group, Dynamic {

    companion object {
        private const val FEET_MOVEMENT_HORIZONTAL = 0.2f
        private const val FEET_MOVEMENT_VERTICAL = 0.01f
        private const val HAND_DISTANCE_FROM_BODY = 0.5f
        private const val HAND_MOVEMENT_HORIZONTAL = 0.1f
        private const val HAND_MOVEMENT_VERTICAL = 0.0125f
        private const val BODY_MOVEMENT_VERTICAL = 0.04f
    }

    private lateinit var gridManager: GridManager
    override val tileWidthMultiplier get() = gridManager.tileWidthMultiplier.value
    override val tileHeightMultiplier get() = gridManager.tileHeightMultiplier.value
    private val bodyParts = listOf(
        // Front right foot
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
            ),
            offsetX = -dimensionX * 0.3f,
            offsetY = -dimensionY * 0.4f,
            dimensionXMultiplier = 0.35f,
            dimensionYMultiplier = 0.4f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetY = -dimensionY * 0.4f + dimensionY * FEET_MOVEMENT_HORIZONTAL * phase.sin
                offsetZ = -dimensionZ * FEET_MOVEMENT_VERTICAL * phase.cos
            },
        ),
        // Front left foot
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
            ),
            offsetX = dimensionX * 0.3f,
            offsetY = -dimensionY * 0.4f,
            dimensionXMultiplier = 0.35f,
            dimensionYMultiplier = 0.4f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetY = -dimensionY * 0.4f + dimensionY * FEET_MOVEMENT_HORIZONTAL * phase.cos
                offsetZ = dimensionZ * FEET_MOVEMENT_VERTICAL * phase.cos
            },
        ),
        // Back right foot
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
            ),
            offsetX = -dimensionX * 0.3f,
            offsetY = dimensionY * 0.4f,
            dimensionXMultiplier = 0.35f,
            dimensionYMultiplier = 0.4f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetY = dimensionY * 0.4f - dimensionY * FEET_MOVEMENT_HORIZONTAL * phase.cos
                offsetZ = -dimensionZ * FEET_MOVEMENT_VERTICAL * phase.cos
            },
        ),
        // Back left foot
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
            ),
            offsetX = dimensionX * 0.3f,
            offsetY = dimensionY * 0.4f,
            dimensionXMultiplier = 0.35f,
            dimensionYMultiplier = 0.4f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetY = dimensionY * 0.4f - dimensionY * FEET_MOVEMENT_HORIZONTAL * phase.sin
                offsetZ = dimensionZ * FEET_MOVEMENT_VERTICAL * phase.cos
            },
        ),
        // Body
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
                extraDepth = -100f,
            ),
            offsetZ = dimensionZ * 0.25f,
            dimensionXMultiplier = 0.75f,
            dimensionYMultiplier = 0.75f,
            dimensionZMultiplier = 0.2f,
            animation = { phase ->
                offsetZ = dimensionZ * 0.25f - dimensionZ * BODY_MOVEMENT_VERTICAL * 0.75f * phase.sin
            },
        ),
        // Tail
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
                extraDepth = -160f,
            ),
            offsetY = dimensionY * 0.4f,
            offsetZ = dimensionZ * 0.5f,
            dimensionXMultiplier = 0.15f,
            dimensionYMultiplier = 0.25f,
            dimensionZMultiplier = 0.1f,
            animation = { phase ->
                offsetZ = dimensionZ * 0.5f - dimensionZ * BODY_MOVEMENT_VERTICAL * phase.sin
                offsetRotationZ = AngleRadians.HalfPi * phase.sin / 8
            },
        ),
        // Head
        BodyPart(
            cube = Cube(
                color = furColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
                extraDepth = -150f,
            ),
            offsetY = -dimensionY * 0.4f,
            offsetZ = dimensionZ * 0.6f,
            dimensionXMultiplier = 0.6f,
            dimensionYMultiplier = 0.5f,
            dimensionZMultiplier = 0.4f,
            animation = { phase ->
                offsetZ = dimensionZ * 0.6f - dimensionZ * BODY_MOVEMENT_VERTICAL * phase.sin
            },
        ),
        // Right eye
        BodyPart(
            cube = Cube(
                color = eyeColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
                extraDepth = -150f,
            ),
            offsetX = -dimensionX * 0.15f,
            offsetY = -dimensionY * 0.8f,
            offsetZ = dimensionZ * 0.8f,
            dimensionXMultiplier = 0.15f,
            dimensionYMultiplier = 0.1f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetZ = dimensionZ * 0.8f - dimensionZ * BODY_MOVEMENT_VERTICAL * phase.sin
            },
        ),
        // Left eye
        BodyPart(
            cube = Cube(
                color = eyeColor,
                shouldDrawShadow = false,
                shouldUseTextures = false,
                extraDepth = -150f,
            ),
            offsetX = dimensionX * 0.15f,
            offsetY = -dimensionY * 0.8f,
            offsetZ = dimensionZ * 0.8f,
            dimensionXMultiplier = 0.15f,
            dimensionYMultiplier = 0.1f,
            dimensionZMultiplier = 0.15f,
            animation = { phase ->
                offsetZ = dimensionZ * 0.8f - dimensionZ * BODY_MOVEMENT_VERTICAL * phase.sin
            },
        ),
    )
    override val actors = bodyParts.map { it.cube }
    private var previousPosition = SceneOffset(positionX, positionY)

    override fun DrawScope.draw() = Unit

    override fun onAdded(kubriko: Kubriko) {
        gridManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        val newPosition = SceneOffset(positionX, positionY)
        if (previousPosition != newPosition) {
            bodyParts.forEach { it.update((deltaTimeInMilliseconds * 1.5f).roundToInt()) }
        }
        previousPosition = newPosition
    }

    override fun updateIsometricBody() = bodyParts.forEach {
        it.updatePosition(
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            dimensionX = dimensionX,
            dimensionY = dimensionY,
            dimensionZ = dimensionZ,
            rotationZ = rotationZ + AngleRadians.HalfPi,
        )
    }

    // TODO: Code duplication
    class BodyPart(
        val cube: Cube,
        var offsetX: SceneUnit = SceneUnit.Zero,
        var offsetY: SceneUnit = SceneUnit.Zero,
        var offsetZ: SceneUnit = SceneUnit.Zero,
        var dimensionXMultiplier: Float = 1f,
        var dimensionYMultiplier: Float = 1f,
        var dimensionZMultiplier: Float = 1f,
        var offsetRotationZ: AngleRadians = AngleRadians.Zero,
        val animation: BodyPart.(AngleRadians) -> Unit = {},
    ) {
        var animationPhase = AngleRadians.Zero

        fun update(deltaTimeInMilliseconds: Int) {
            animationPhase += (deltaTimeInMilliseconds / 200f).rad
            animation(animationPhase)
        }

        fun updatePosition(
            positionX: SceneUnit,
            positionY: SceneUnit,
            positionZ: SceneUnit,
            dimensionX: SceneUnit,
            dimensionY: SceneUnit,
            dimensionZ: SceneUnit,
            rotationZ: AngleRadians,
        ) = cube.update(
            positionX = positionX + offsetX * rotationZ.cos - offsetY * rotationZ.sin,
            positionY = positionY + offsetX * rotationZ.sin + offsetY * rotationZ.cos,
            positionZ = positionZ + offsetZ,
            dimensionX = dimensionX * dimensionXMultiplier,
            dimensionY = dimensionY * dimensionYMultiplier,
            dimensionZ = dimensionZ * dimensionZMultiplier,
            rotationZ = rotationZ + offsetRotationZ,
        )
    }
}