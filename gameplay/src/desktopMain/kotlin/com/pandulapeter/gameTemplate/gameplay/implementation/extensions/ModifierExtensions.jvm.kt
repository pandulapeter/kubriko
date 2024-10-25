package com.pandulapeter.gameTemplate.gameplay.implementation.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.gameTemplate.engine.Engine

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseZoom(): Modifier = this
    .onPointerEvent(PointerEventType.Scroll) {
        Engine.get().viewportManager.multiplyScaleFactor(
            scaleFactor = 1f - it.changes.first().scrollDelta.y * 0.05f
        )
    }