package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset

fun Offset.toSceneOffset(viewportManager: ViewportManager): SceneOffset = toSceneOffset(
    viewportCenter = viewportManager.cameraPosition.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

fun Offset.toSceneOffset(
    viewportCenter: SceneOffset,
    viewportSize: Size,
    viewportScaleFactor: Float,
): SceneOffset = viewportCenter + SceneOffset(
    x = (x - viewportSize.width / 2).sceneUnit,
    y = (y - viewportSize.height / 2).sceneUnit,
) / viewportScaleFactor
