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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.GridMap
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.actor.VolumetricCuboidRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.manager.VolumetricRenderManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.utility.IsometricGridLineCache
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.utility.drawIsometricGrid
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.description
import org.jetbrains.compose.resources.stringResource

private const val JOYSTICK_ENABLED = true

@Composable
internal fun IsometricGraphicsContent(
    stateHolder: IsometricGraphicsDemoStateHolderImpl,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) = Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
) {
    val worldRotationState = stateHolder.volumetricRenderManager.worldRotation.collectAsState()
    val offsetState = stateHolder.controlManager.cameraOffset.collectAsState()
    val zoomState = stateHolder.volumetricRenderManager.zoom.collectAsState()
    val tiltState = stateHolder.volumetricRenderManager.tilt.collectAsState()
    val joystickOrigin = stateHolder.controlOverlayManager.joystickOrigin.collectAsState()
    val joystickDirection = stateHolder.controlOverlayManager.joystickDirection.collectAsState()
    val joystickSpeedFactor = stateHolder.controlOverlayManager.joystickSpeedFactor.collectAsState()
    val image = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        while (image.value == null) {
            image.value = stateHolder.textureManager.resolveTexture("map")
            if (image.value == null) {
                delay(50L.milliseconds)
            }
        }
    }
    val gridMap = remember(image.value) { image.value?.let(::GridMap) }
    val gridLinesPath = remember { Path() }
    val gridLineCache = remember { IsometricGridLineCache() }
    val isoMatrix = remember { Matrix() }
    // Square caps close the corners like Round did, without tessellating an arc per segment end.
    val stroke = remember { Stroke(width = VolumetricCuboidRenderer.STROKE_WIDTH, cap = StrokeCap.Square) }
    val size = stateHolder.volumetricViewportManager.size.collectAsState()
    val density = LocalDensity.current
    val currentOrigin = joystickOrigin.value
    val currentDirection = joystickDirection.value
    val joystickMaxRadiusPx = with(density) { 64.dp.toPx() }
    val joystickVisualRadiusPx = joystickMaxRadiusPx // Visual background radius (128dp diameter / 2 = 64dp)
    val joystickTriggerRadiusPx = joystickVisualRadiusPx * 2f // Touch target is 1.5x the visual size
    val paddingPx = with(density) { 16.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingInsets = WindowInsets.safeDrawing
    val leftInsetPx = safeDrawingInsets.getLeft(density, layoutDirection).toFloat()
    val bottomInsetPx = safeDrawingInsets.getBottom(density).toFloat()

    val defaultJoystickPosition = remember(leftInsetPx, bottomInsetPx, paddingPx, joystickVisualRadiusPx, size.value) {
        Offset(
            x = leftInsetPx + paddingPx + joystickVisualRadiusPx,
            y = size.value.height - bottomInsetPx - paddingPx - joystickVisualRadiusPx
        )
    }
    val isJoystickPositionInitialized = remember { mutableStateOf(false) }
    val animatedJoystickOrigin by animateOffsetAsState(
        targetValue = currentOrigin ?: defaultJoystickPosition,
        animationSpec = if (isJoystickPositionInitialized.value) spring() else snap(),
        label = "joystickOrigin"
    )
    val animatedJoystickAlpha by animateFloatAsState(
        targetValue = if (currentOrigin != null) 1f else 0.5f,
        label = "joystickAlpha"
    )
    if (!isJoystickPositionInitialized.value && size.value.width > 0f && size.value.height > 0f) {
        SideEffect { isJoystickPositionInitialized.value = true }
    }
    val travelRadiusPx = with(density) { 56.dp.toPx() }
    val animatedKnobOffset by animateOffsetAsState(
        targetValue = if (currentDirection != null) {
            Offset(
                x = currentDirection.cos * travelRadiusPx * joystickSpeedFactor.value,
                y = currentDirection.sin * travelRadiusPx * joystickSpeedFactor.value
            )
        } else {
            Offset.Zero
        },
        label = "knobOffset"
    )

    SideEffect {
        stateHolder.controlOverlayManager.isJoystickEnabled = JOYSTICK_ENABLED
        stateHolder.controlOverlayManager.joystickMaxRadiusPx = joystickMaxRadiusPx
        stateHolder.controlOverlayManager.joystickVisualRadiusPx = joystickVisualRadiusPx
        stateHolder.controlOverlayManager.joystickTriggerRadiusPx = joystickTriggerRadiusPx
        stateHolder.controlOverlayManager.paddingPx = paddingPx
        stateHolder.controlOverlayManager.leftInsetPx = leftInsetPx
        stateHolder.controlOverlayManager.bottomInsetPx = bottomInsetPx
    }
    KubrikoViewport(
        modifier = Modifier
            .drawBehind {
                val offset = offsetState.value
                val worldRotation = worldRotationState.value
                val zoom = zoomState.value
                val tilt = tiltState.value
                drawIsometricGrid(
                    gridLinesPath = gridLinesPath,
                    isoMatrix = isoMatrix,
                    gridColor = Color.Black,
                    tileWidth = 100.sceneUnit,
                    tileHeight = 100.sceneUnit,
                    cameraPosition = offset,
                    worldRotation = worldRotation,
                    zoom = zoom * 2f,
                    tilt = tilt,
                    gridMap = gridMap,
                    stroke = stroke,
                    size = size.value,
                    focusHeight = VolumetricRenderManager.FOCUS_HEIGHT,
                    lineCache = gridLineCache,
                )
            },
        kubriko = stateHolder.isometricKubriko,
    )
    if (JOYSTICK_ENABLED) {
        Box {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val radius = with(density) { 64.dp.toPx() }
                        translationX = animatedJoystickOrigin.x - radius
                        translationY = animatedJoystickOrigin.y - radius
                    }
                    .size(128.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = animatedJoystickAlpha * 0.75f),
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black.copy(alpha = animatedJoystickAlpha),
                        shape = CircleShape,
                    )
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val radius = with(density) { 20.dp.toPx() }
                        translationX = animatedJoystickOrigin.x + animatedKnobOffset.x - radius
                        translationY = animatedJoystickOrigin.y + animatedKnobOffset.y - radius
                    }
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = animatedJoystickAlpha),
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = Color.Black.copy(alpha = animatedJoystickAlpha),
                        shape = CircleShape,
                    )
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoPanel(
            text = stringResource(Res.string.description),
            isVisible = StateHolder.isInfoPanelVisible.value,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            MiniMap(stateHolder = stateHolder, gridMap = gridMap)
        }
    }
    LoadingOverlay(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shouldShowLoadingIndicator = stateHolder.shouldShowLoadingIndicator.collectAsState().value,
    )
}
