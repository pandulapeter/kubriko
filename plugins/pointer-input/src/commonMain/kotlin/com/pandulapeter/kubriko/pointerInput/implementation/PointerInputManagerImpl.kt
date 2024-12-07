package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import kotlinx.coroutines.flow.map

internal class PointerInputManagerImpl : PointerInputManager() {

    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private val pointerInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<PointerInputAware>() }.asStateFlow(emptyList())
    }
    private var lastPointerOffset = Offset.Zero

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
        stateManager = kubriko.require<StateManager>()
    }

    override fun getModifier(layerIndex: Int?) = Modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                    lastPointerOffset = change.position
                    pointerInputAwareActors.value.forEach { it.onPointerPress(lastPointerOffset) }
                },
                onDragEnd = {
                    pointerInputAwareActors.value.forEach { it.onPointerReleased(lastPointerOffset) }
                },
                onDragCancel = {
                    pointerInputAwareActors.value.forEach { it.onPointerReleased(lastPointerOffset) }
                }
            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { screenOffset ->
                    lastPointerOffset = screenOffset
                    pointerInputAwareActors.value.forEach { it.onPointerReleased(screenOffset) }
                }
            )
        }
}