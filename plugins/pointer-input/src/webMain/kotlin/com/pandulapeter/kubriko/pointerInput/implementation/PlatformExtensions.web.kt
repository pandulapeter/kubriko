/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.browser.window
import org.w3c.dom.events.WheelEvent

internal actual fun setPointerPosition(
    platform: MetadataManager.Platform,
    offset: Offset,
    densityMultiplier: Float,
) = false

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.gestureDetector(
    onDragDetected: (Offset) -> Unit,
    onZoomDetected: (Offset, Float) -> Unit,
) = pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, _ ->
        onDragDetected(pan)
        onZoomDetected(centroid, zoom)
    }
}.onPointerEvent(PointerEventType.Scroll) {
    val change = it.changes.first()
    onZoomDetected(
        change.position,
        scrollZoomFactor(change.scrollDelta.y * (it.nativeEvent as? WheelEvent).pixelsPerDeltaUnit(), 0.005f),
    )
}

/** Compose passes the browser's raw `deltaY` on, whatever unit its `deltaMode` says it is in. */
private fun WheelEvent?.pixelsPerDeltaUnit() = when (this?.deltaMode) {
    WheelEvent.DOM_DELTA_LINE -> PIXELS_PER_LINE
    WheelEvent.DOM_DELTA_PAGE -> window.innerHeight.toFloat()
    else -> 1f
}

/** Chrome's own line height, for its 3-line, 100 pixel notch. */
private const val PIXELS_PER_LINE = 100f / 3f

// TODO: https://youtrack.jetbrains.com/issue/CMP-6957/Web.-detectTransformGestures-doesnt-catch-zoom-and-rotation-gestures
internal actual val isMultiTouchEnabled = true