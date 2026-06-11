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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.rotateTowards
import com.pandulapeter.kubriko.helpers.extensions.shortestDeltaTo
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager.ControlManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.CuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.RenderableCuboidModel
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.MiniMapMarker
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.actor.PlanarCuboidModelRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.model.ProjectionPlane
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

class MainCharacter(
    cuboidModel: CuboidModel,
    position: Vec3,
    textureResolver: (String) -> ImageBitmap?,
) : PlanarCuboidModelRenderer(
    renderableCuboidModel = RenderableCuboidModel(
        id = "${Character.ID_PREFIX}_main",
        cuboidModel = cuboidModel,
        position = position,
    ),
    projectionPlane = ProjectionPlane.XY,
    textureResolver = textureResolver,
    miniMapMarker = MainCharacterMarker,
    isPreferredMiniMapMarker = { it == "Head" },
    isDrawn = false,
) {
    private lateinit var controlManager: ControlManager
    private val maximumSpeed = SceneUnit.Unit * 0.5f
    private val minimumSpeed = maximumSpeed * 0.5f
    private var goalRotation = renderableCuboidModel.rotationZ
    private var pendingGoalRotation = renderableCuboidModel.rotationZ
    private var pendingGoalRotationMs = 0
    private var isPlayingWalkAnimation = false
    private var currentIdleAnimationName = "Idle-01"
    private var currentSpeed = SceneUnit.Zero
    private var controlReleasedDurationMs = 0
    private var idleCrossfadeRemainingMs = 0
    private var idleCrossfadeInitialSpeed = SceneUnit.Zero

    override fun onAdded(kubriko: Kubriko) {
        controlManager = kubriko.get()
        controlManager.onCameraOffsetChanged(renderableCuboidModel.position.positionXY)
        playAnimation(name = currentIdleAnimationName, shouldRepeat = true)
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        val controlDirection = controlManager.controlDirection.value
        if (controlDirection == null) updateOnNoInput(deltaTimeInMilliseconds)
        else updateOnInput(controlDirection, deltaTimeInMilliseconds)
        move(deltaTimeInMilliseconds)
        controlManager.onCameraOffsetChanged(renderableCuboidModel.position.positionXY)
        advanceAnimations(deltaTimeInMilliseconds)
        super.update(deltaTimeInMilliseconds)
    }

    private fun updateOnNoInput(deltaTimeInMilliseconds: Int) {
        controlReleasedDurationMs += deltaTimeInMilliseconds
        if (isPlayingWalkAnimation && controlReleasedDurationMs >= IDLE_DEBOUNCE_MS) {
            isPlayingWalkAnimation = false
            idleCrossfadeRemainingMs = CROSSFADE_DURATION_MS
            idleCrossfadeInitialSpeed = currentSpeed
            currentIdleAnimationName = if (Random.nextBoolean()) "Idle-01" else "Idle-02"
            crossfadeToAnimation(name = currentIdleAnimationName, shouldRepeat = true, durationMs = CROSSFADE_DURATION_MS)
        }
        if (!isPlayingWalkAnimation) {
            if (idleCrossfadeRemainingMs > 0) {
                idleCrossfadeRemainingMs -= deltaTimeInMilliseconds
                currentSpeed = if (idleCrossfadeRemainingMs <= 0) {
                    idleCrossfadeRemainingMs = 0
                    SceneUnit.Zero
                } else {
                    idleCrossfadeInitialSpeed * (idleCrossfadeRemainingMs.toFloat() / CROSSFADE_DURATION_MS)
                }
            } else {
                currentSpeed = SceneUnit.Zero
            }
        }
    }

    private fun updateOnInput(controlDirection: AngleRadians, deltaTimeInMilliseconds: Int) {
        controlReleasedDurationMs = 0
        idleCrossfadeRemainingMs = 0
        if (pendingGoalRotation.shortestDeltaTo(controlDirection).raw.absoluteValue > DIRECTION_DEBOUNCE_THRESHOLD) {
            pendingGoalRotationMs = 0
        }
        pendingGoalRotation = controlDirection
        pendingGoalRotationMs += deltaTimeInMilliseconds
        if (pendingGoalRotationMs >= DIRECTION_DEBOUNCE_MS || !isPlayingWalkAnimation) {
            goalRotation = pendingGoalRotation
        }
        renderableCuboidModel.rotationZ = renderableCuboidModel.rotationZ.rotateTowards(
            target = goalRotation,
            maxDelta = (AngleRadians.HalfPi * 0.0025f + renderableCuboidModel.rotationZ.shortestDeltaTo(goalRotation).raw.absoluteValue.rad * 0.0075f) * deltaTimeInMilliseconds,
        )
        val rawTargetSpeed = maximumSpeed * controlManager.controlSpeedFactor.value
        val targetSpeed = if (rawTargetSpeed < minimumSpeed) minimumSpeed else rawTargetSpeed
        if (currentSpeed < targetSpeed) {
            currentSpeed += maximumSpeed * 0.005f * deltaTimeInMilliseconds
        } else if (currentSpeed > targetSpeed) {
            currentSpeed -= maximumSpeed * 0.005f * deltaTimeInMilliseconds
            if (currentSpeed < SceneUnit.Zero) currentSpeed = SceneUnit.Zero
        }
        if (!isPlayingWalkAnimation) {
            isPlayingWalkAnimation = true
            crossfadeToAnimation(name = "Walking", shouldRepeat = true, durationMs = CROSSFADE_DURATION_MS)
        }
    }

    private fun move(deltaTimeInMilliseconds: Int) {
        if (currentSpeed == SceneUnit.Zero) return
        val moveCos = (renderableCuboidModel.rotationZ - AngleRadians.Pi).cos
        val moveSin = (renderableCuboidModel.rotationZ - AngleRadians.Pi).sin
        renderableCuboidModel.position.x -= currentSpeed * moveCos * deltaTimeInMilliseconds
        renderableCuboidModel.position.y -= currentSpeed * moveSin * deltaTimeInMilliseconds
    }

    private fun advanceAnimations(deltaTimeInMilliseconds: Int) {
        val walkAnimationDelta = (deltaTimeInMilliseconds * currentSpeed.raw * 3f).roundToInt()
        if (walkAnimationDelta > 0) {
            advanceAnimation(name = "Walking", deltaTimeInMilliseconds = walkAnimationDelta)
        }
        if (!isPlayingWalkAnimation) {
            advanceAnimation(name = currentIdleAnimationName, deltaTimeInMilliseconds = deltaTimeInMilliseconds)
        }
    }

    private companion object {
        const val CROSSFADE_DURATION_MS = 150
        const val IDLE_DEBOUNCE_MS = 150
        const val DIRECTION_DEBOUNCE_MS = 50
        const val DIRECTION_DEBOUNCE_THRESHOLD = 0.1f

        private val MainCharacterMarker = object : MiniMapMarker {
            override fun DrawScope.draw(
                centerX: Float,
                centerY: Float,
                halfWidth: Float,
                halfHeight: Float,
                rotation: Float,
                color: Color,
                stroke: Stroke
            ) {
                rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
                    val path = Path().apply {
                        arcTo(
                            rect = Rect(centerX - halfWidth * 2, centerY - halfWidth * 2, centerX + halfWidth * 2, centerY + halfWidth * 2),
                            startAngleDegrees = 45f,
                            sweepAngleDegrees = 270f,
                            forceMoveTo = false
                        )
                        lineTo(centerX + halfWidth * 3.2f, centerY)
                        close()
                    }
                    drawPath(path, Color.White)
                    drawPath(path, Color.Black, style = stroke)
                }
            }
        }
    }
}
