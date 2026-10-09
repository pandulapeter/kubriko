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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.gameplay.resources.TextureResolver
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

private val CROSSFADE_SETTLE_DELAY = 350L.milliseconds

private val JoystickBaseRadius = 64.dp
private val JoystickKnobRadius = 20.dp

@Composable
internal fun IsometricGraphicsContent(
    stateHolder: IsometricGraphicsDemoStateHolderImpl,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) = Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
) {
    // Tick-cadence snapshot of the values the world renders with, so the grid steps in sync with the
    // rest of the scene when the frame rate is limited instead of gliding smoothly at display rate.
    val renderState = stateHolder.volumetricRenderManager.renderState.collectAsState()
    val joystickOrigin = stateHolder.controlOverlayManager.joystickOrigin.collectAsState()
    val joystickDirection = stateHolder.controlOverlayManager.joystickDirection.collectAsState()
    val joystickSpeedFactor = stateHolder.controlOverlayManager.joystickSpeedFactor.collectAsState()
    // Held back past the Showcase crossfade so the viewports' heavy first frame doesn't stall it.
    val isReadyToRender = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CROSSFADE_SETTLE_DELAY)
        isReadyToRender.value = true
    }
    val image = rememberMapTexture(
        textureResolver = stateHolder.textureResolver,
        isReadyToRender = isReadyToRender.value,
    )
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
    val joystickMaxRadiusPx = with(density) { JoystickBaseRadius.toPx() }
    val joystickVisualRadiusPx = joystickMaxRadiusPx // Visual background radius (128dp diameter / 2 = 64dp)
    val joystickTriggerRadiusPx = joystickVisualRadiusPx * 2f // Touch target is twice the visual radius
    val paddingPx = with(density) { 16.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val leftInsetPx = windowInsets.getLeft(density, layoutDirection).toFloat()
    val bottomInsetPx = windowInsets.getBottom(density).toFloat()
    val joystickLayout = remember(joystickMaxRadiusPx, paddingPx, leftInsetPx, bottomInsetPx) {
        JoystickLayout(
            isEnabled = JOYSTICK_ENABLED,
            visualRadiusPx = joystickVisualRadiusPx,
            maxRadiusPx = joystickMaxRadiusPx,
            triggerRadiusPx = joystickTriggerRadiusPx,
            paddingPx = paddingPx,
            leftInsetPx = leftInsetPx,
            bottomInsetPx = bottomInsetPx,
        )
    }

    val defaultJoystickPosition = remember(joystickLayout, size.value) { joystickLayout.center(size.value.height) }
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
        stateHolder.controlOverlayManager.joystickLayout = joystickLayout
    }
    if (isReadyToRender.value) {
        KubrikoViewport(
            modifier = Modifier
                .isometricGrid(
                    renderState = renderState,
                    gridLinesPath = gridLinesPath,
                    isoMatrix = isoMatrix,
                    gridMap = gridMap,
                    stroke = stroke,
                    size = size,
                    lineCache = gridLineCache,
                ),
            kubriko = stateHolder.isometricKubriko,
        )
        if (JOYSTICK_ENABLED) {
            Joystick(
                origin = { animatedJoystickOrigin },
                knobOffset = { animatedKnobOffset },
                alpha = animatedJoystickAlpha,
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
        if (isReadyToRender.value) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MiniMap(
                    logicKubriko = stateHolder.logicKubriko,
                    logicVisibleActors = stateHolder.logicVisibleActors,
                    worldRotation = stateHolder.volumetricRenderManager.worldRotation,
                    cameraOffset = stateHolder.controlManager.cameraOffset,
                    gridMap = gridMap,
                )
            }
        }
    }
    LoadingOverlay(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shouldShowLoadingIndicator = !isReadyToRender.value || stateHolder.shouldShowLoadingIndicator.collectAsState().value,
    )
}

@Composable
private fun rememberMapTexture(
    textureResolver: TextureResolver,
    isReadyToRender: Boolean,
): State<ImageBitmap?> {
    val image = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(isReadyToRender) {
        if (!isReadyToRender) return@LaunchedEffect
        while (image.value == null) {
            image.value = textureResolver.resolveTexture("map")
            if (image.value == null) {
                delay(50L.milliseconds)
            }
        }
    }
    return image
}

private fun Modifier.isometricGrid(
    renderState: State<VolumetricRenderManager.RenderState>,
    gridLinesPath: Path,
    isoMatrix: Matrix,
    gridMap: GridMap?,
    stroke: Stroke,
    size: State<Size>,
    lineCache: IsometricGridLineCache,
) = drawBehind {
    val state = renderState.value
    val offset = state.cameraOffset
    val worldRotation = state.worldRotation
    val zoom = state.zoom
    val tilt = state.tilt
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
        lineCache = lineCache,
    )
}

@Composable
private fun Joystick(
    origin: () -> Offset,
    knobOffset: () -> Offset,
    alpha: Float,
) = Box {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .graphicsLayer {
                val radius = with(density) { JoystickBaseRadius.toPx() }
                translationX = origin().x - radius
                translationY = origin().y - radius
            }
            .size(JoystickBaseRadius * 2)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = alpha * 0.75f),
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = Color.Black.copy(alpha = alpha),
                shape = CircleShape,
            )
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                val radius = with(density) { JoystickKnobRadius.toPx() }
                translationX = origin().x + knobOffset().x - radius
                translationY = origin().y + knobOffset().y - radius
            }
            .size(JoystickKnobRadius * 2)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = Color.Black.copy(alpha = alpha),
                shape = CircleShape,
            )
    )
}
