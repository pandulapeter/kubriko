/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.extensions.angleTowards
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.keyboardInput.extensions.hasDown
import com.pandulapeter.kubriko.keyboardInput.extensions.hasDownLeft
import com.pandulapeter.kubriko.keyboardInput.extensions.hasDownRight
import com.pandulapeter.kubriko.keyboardInput.extensions.hasLeft
import com.pandulapeter.kubriko.keyboardInput.extensions.hasRight
import com.pandulapeter.kubriko.keyboardInput.extensions.hasUp
import com.pandulapeter.kubriko.keyboardInput.extensions.hasUpLeft
import com.pandulapeter.kubriko.keyboardInput.extensions.hasUpRight
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import com.pandulapeter.kubriko.types.TargetFrameRate
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.manager.VolumetricRenderManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.logic.manager.ControlManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.hypot
import kotlin.math.sqrt

internal class ControlOverlayManager(
    private val controlManager: ControlManager,
    private val logicViewportManager: ViewportManager,
) : Manager(), KeyboardInputAware, PointerInputAware {

    private val viewportManager by manager<ViewportManager>()
    private val volumetricRenderManager by manager<VolumetricRenderManager>()
    private val stateManager by manager<StateManager>()
    var isJoystickEnabled: Boolean = true
    private var joystickPointerId: PointerId? = null
    private var _joystickOrigin = MutableStateFlow<Offset?>(null)
    val joystickOrigin = _joystickOrigin.asStateFlow()
    private var _joystickDirection = MutableStateFlow<AngleRadians?>(null)
    val joystickDirection = _joystickDirection.asStateFlow()
    private val joystickDeadZoneSq = 100f
    var joystickMaxRadiusPx: Float = 200f
    var joystickVisualRadiusPx: Float = 0f
    var joystickTriggerRadiusPx: Float = 0f
    var paddingPx: Float = 0f
    var leftInsetPx: Float = 0f
    var bottomInsetPx: Float = 0f
    private var _joystickSpeedFactor = MutableStateFlow(0f)
    val joystickSpeedFactor = _joystickSpeedFactor.asStateFlow()
    private var cameraPointerId: PointerId? = null
    private var cameraLastPosition: Offset? = null
    private var secondaryCameraPointerId: PointerId? = null
    private var secondaryCameraLastPosition: Offset? = null
    private val keyboardMovementSpeed = SceneUnit.Unit
    private var millisSinceLastInteraction = 0
    private var isFrameRateThrottled = false

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
        // Without this the isometric instance keeps rendering at full rate while the window is unfocused.
        stateManager.isFocused
            .onEach(stateManager::updateIsRunning)
            .launchIn(scope)
    }

    // Power saving: with no input for a while the scene is static except for the idle animation,
    // so both Kubriko instances drop to a low fixed frame rate. Any interaction restores the device
    // maximum on the next frame. A held-but-motionless joystick produces no pointer events while
    // the character keeps walking, hence the active-pointer check.
    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (joystickPointerId != null || cameraPointerId != null || secondaryCameraPointerId != null) {
            millisSinceLastInteraction = 0
        } else {
            millisSinceLastInteraction += deltaTimeInMilliseconds
        }
        val shouldThrottle = millisSinceLastInteraction >= IDLE_FRAME_RATE_TIMEOUT_MS
        if (shouldThrottle != isFrameRateThrottled) {
            isFrameRateThrottled = shouldThrottle
            val targetFrameRate = if (shouldThrottle) IDLE_TARGET_FRAME_RATE else TargetFrameRate.DisplayDefault
            viewportManager.setTargetFrameRate(targetFrameRate)
            logicViewportManager.setTargetFrameRate(targetFrameRate)
        }
    }

    private fun registerInteraction() {
        millisSinceLastInteraction = 0
        if (isFrameRateThrottled) {
            isFrameRateThrottled = false
            viewportManager.setTargetFrameRate(TargetFrameRate.DisplayDefault)
            logicViewportManager.setTargetFrameRate(TargetFrameRate.DisplayDefault)
        }
    }

    // TODO: Bad and lazy
    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (activeKeys.isNotEmpty()) {
            registerInteraction()
        }
        controlManager.onControlDirectionChanged(
            when (activeKeys.directionState) {
                KeyboardDirectionState.NONE -> null
                KeyboardDirectionState.LEFT -> SceneOffset(-keyboardMovementSpeed, SceneUnit.Zero).calculateMovementDirection()
                KeyboardDirectionState.UP_LEFT -> SceneOffset(-keyboardMovementSpeed, -keyboardMovementSpeed).calculateMovementDirection()
                KeyboardDirectionState.UP -> SceneOffset(SceneUnit.Zero, -keyboardMovementSpeed).calculateMovementDirection()
                KeyboardDirectionState.UP_RIGHT -> SceneOffset(keyboardMovementSpeed, -keyboardMovementSpeed).calculateMovementDirection()
                KeyboardDirectionState.RIGHT -> SceneOffset(keyboardMovementSpeed, SceneUnit.Zero).calculateMovementDirection()
                KeyboardDirectionState.DOWN_RIGHT -> SceneOffset(keyboardMovementSpeed, keyboardMovementSpeed).calculateMovementDirection()
                KeyboardDirectionState.DOWN -> SceneOffset(SceneUnit.Zero, keyboardMovementSpeed).calculateMovementDirection()
                KeyboardDirectionState.DOWN_LEFT -> SceneOffset(-keyboardMovementSpeed, keyboardMovementSpeed).calculateMovementDirection()
            }
        )
    }

    override fun onPointerZoom(position: Offset, factor: Float) {
        registerInteraction()
        if (secondaryCameraPointerId == null) {
            volumetricRenderManager.multiplyWorldZoom(factor)
        }
    }

    private fun Offset.isWithinJoystickRegion(): Boolean {
        val size = viewportManager.size.value
        val centerX = leftInsetPx + paddingPx + joystickVisualRadiusPx
        val centerY = size.height - bottomInsetPx - paddingPx - joystickVisualRadiusPx
        // Clamp toward the bottom-left screen corner so touches in the inset / edge strip
        // (left of and below the visual center) still trigger the joystick. This keeps it
        // usable outside the system window insets while staying a bounded circle toward up/right.
        val dx = (x - centerX).coerceAtLeast(0f)
        val dy = (y - centerY).coerceAtMost(0f)
        return (dx * dx + dy * dy) <= (joystickTriggerRadiusPx * joystickTriggerRadiusPx)
    }

    override fun onPointerPressed(pointerId: PointerId, screenOffset: Offset) {
        registerInteraction()
        val isBottomLeftQuadrant = isJoystickEnabled && screenOffset.isWithinJoystickRegion()
        if (joystickPointerId == null && isBottomLeftQuadrant) {
            joystickPointerId = pointerId
            _joystickOrigin.value = screenOffset
        } else if (cameraPointerId == null) {
            cameraPointerId = pointerId
            cameraLastPosition = screenOffset
        } else if (secondaryCameraPointerId == null) {
            secondaryCameraPointerId = pointerId
            secondaryCameraLastPosition = screenOffset
        }
    }

    override fun onPointerOffsetChanged(pointerId: PointerId?, screenOffset: Offset) {
        if (pointerId != null) {
            registerInteraction()
        }
        when (pointerId) {
            joystickPointerId -> {
                _joystickOrigin.value?.let { origin ->
                    val deltaX = screenOffset.x - origin.x
                    val deltaY = screenOffset.y - origin.y
                    val distanceSq = (deltaX * deltaX) + (deltaY * deltaY)
                    if (distanceSq > joystickDeadZoneSq) {
                        val joystickOffset = SceneOffset(
                            x = deltaX.sceneUnit,
                            y = deltaY.sceneUnit,
                        )
                        val speedFactor = (sqrt(distanceSq) / joystickMaxRadiusPx).coerceIn(0f, 1f)
                        _joystickSpeedFactor.value = speedFactor
                        _joystickDirection.value = SceneOffset.Zero.angleTowards(joystickOffset.normalized())
                        controlManager.onControlDirectionChanged(joystickOffset.calculateMovementDirection(), speedFactor)
                    } else {
                        _joystickSpeedFactor.value = 0f
                        controlManager.onControlDirectionChanged(null)
                    }
                }
            }

            cameraPointerId -> {
                val newPos = screenOffset
                val secPos = secondaryCameraLastPosition
                if (secondaryCameraPointerId != null && secPos != null) {
                    cameraLastPosition?.let { lastPos ->
                        val oldDistance = getDistance(lastPos, secPos)
                        val newDistance = getDistance(newPos, secPos)
                        if (oldDistance > 0f) {
                            volumetricRenderManager.multiplyWorldZoom(newDistance / oldDistance)
                        }
                    }
                } else {
                    cameraLastPosition?.let { lastPos ->
                        val deltaX = (newPos.x - lastPos.x) * 0.5f
                        val deltaY = (newPos.y - lastPos.y) * 0.5f
                        volumetricRenderManager.addToWorldRotation(-deltaX.rad * 0.005f)
                        volumetricRenderManager.addToWorldTilt(deltaY * 0.005f)
                    }
                }
                cameraLastPosition = newPos
            }

            secondaryCameraPointerId -> {
                val newPos = screenOffset
                val primPos = cameraLastPosition
                if (cameraPointerId != null && primPos != null) {
                    secondaryCameraLastPosition?.let { lastSecPos ->
                        val oldDistance = getDistance(primPos, lastSecPos)
                        val newDistance = getDistance(primPos, newPos)
                        if (oldDistance > 0f) {
                            volumetricRenderManager.multiplyWorldZoom(newDistance / oldDistance)
                        }
                    }
                }
                secondaryCameraLastPosition = newPos
            }
        }
    }

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        registerInteraction()
        when (pointerId) {
            joystickPointerId -> {
                joystickPointerId = null
                _joystickOrigin.value = null
                _joystickDirection.value = null
                _joystickSpeedFactor.value = 0f
                controlManager.onControlDirectionChanged(null)
            }

            cameraPointerId -> {
                cameraPointerId = null
                cameraLastPosition = null
                if (secondaryCameraPointerId != null) {
                    cameraPointerId = secondaryCameraPointerId
                    cameraLastPosition = secondaryCameraLastPosition

                    secondaryCameraPointerId = null
                    secondaryCameraLastPosition = null
                }
            }

            secondaryCameraPointerId -> {
                secondaryCameraPointerId = null
                secondaryCameraLastPosition = null
            }
        }
    }

    private fun getDistance(p1: Offset, p2: Offset) = hypot(p1.x - p2.x, p1.y - p2.y)

    private companion object {
        const val IDLE_FRAME_RATE_TIMEOUT_MS = 2000
        val IDLE_TARGET_FRAME_RATE = TargetFrameRate.DisplayDivider(2)
    }

    private fun SceneOffset.calculateMovementDirection() =
        SceneOffset.Zero.angleTowards(normalized()) - volumetricRenderManager.worldRotation.value - AngleRadians.HalfPi / 2f

    private val Set<Key>.directionState
        get() = when {
            isEmpty() -> KeyboardDirectionState.NONE
            (hasLeft && !hasRight && !hasUp && !hasDown) || (hasLeft && !hasRight && hasUp && hasDown) -> KeyboardDirectionState.LEFT
            hasUpLeft && !hasDown && !hasRight -> KeyboardDirectionState.UP_LEFT
            (hasUp && !hasDown && !hasLeft && !hasRight) || (hasUp && !hasDown && hasLeft && hasRight) -> KeyboardDirectionState.UP
            hasUpRight && !hasDown && !hasLeft -> KeyboardDirectionState.UP_RIGHT
            (hasRight && !hasLeft && !hasUp && !hasDown) || (hasRight && !hasLeft && hasUp && hasDown) -> KeyboardDirectionState.RIGHT
            hasDownRight && !hasUp && !hasLeft -> KeyboardDirectionState.DOWN_RIGHT
            (hasDown && !hasUp && !hasLeft && !hasRight) || (hasDown && !hasUp && hasLeft && hasRight) -> KeyboardDirectionState.DOWN
            hasDownLeft && !hasUp && !hasRight -> KeyboardDirectionState.DOWN_LEFT
            else -> KeyboardDirectionState.NONE
        }
}