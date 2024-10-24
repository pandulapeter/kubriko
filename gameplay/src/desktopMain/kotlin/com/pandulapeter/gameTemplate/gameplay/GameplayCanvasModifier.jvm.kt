package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.gameTemplate.engine.getEngine

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.getGameplayCanvasModifier(): Modifier = this
    .onPointerEvent(PointerEventType.Scroll) {
        getEngine().addToCameraScaleFactor(-it.changes.first().scrollDelta.y * 0.02f)
    }