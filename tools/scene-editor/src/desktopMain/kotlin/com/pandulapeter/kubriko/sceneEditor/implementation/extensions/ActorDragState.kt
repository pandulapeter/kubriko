/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.extensions

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

private const val MINIMUM_INTERACTIVE_SCALE = 0.05f

/**
 * The state of one editor's actor drag gesture, written and read only from its pointer callbacks on the UI thread.
 */
internal class ActorDragState {
    var startOffset: SceneOffset? = null
    var isDragging = false
    var dragStartMouseSceneOffset = SceneOffset.Zero
    var dragStartScale = Scale.Unit
    var dragStartRotation = AngleRadians.Zero
    var dragStartPointerAngle = AngleRadians.Zero
}

internal fun draggedScale(
    startScale: Scale,
    mouseDelta: SceneOffset,
    size: SceneSize,
    minimumScale: Float = MINIMUM_INTERACTIVE_SCALE,
): Scale {
    val width = size.width.raw
    val height = size.height.raw
    return Scale(
        horizontal = if (width > 0f) {
            (startScale.horizontal + mouseDelta.x.raw / width).coerceAtLeast(minimumScale)
        } else {
            startScale.horizontal
        },
        vertical = if (height > 0f) {
            (startScale.vertical + mouseDelta.y.raw / height).coerceAtLeast(minimumScale)
        } else {
            startScale.vertical
        },
    )
}

internal fun draggedRotation(
    startRotation: AngleRadians,
    startPointerAngle: AngleRadians,
    currentPointerAngle: AngleRadians,
) = startRotation + (currentPointerAngle - startPointerAngle)
