package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class PointerInputManagerImpl : PointerInputManager() {

    // TODO: Implement multi-touch support
    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private val pointerInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<PointerInputAware>() }.asStateFlow(emptyList())
    }
    private var pointerOffset = MutableStateFlow<Offset?>(null)
    private var isPointerPressed = MutableStateFlow(false)

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
        stateManager = kubriko.require<StateManager>()
        stateManager.isFocused
            .filterNot { it }
            .onEach { isPointerPressed.value = false }
            .launchIn(scope)
        pointerOffset
            .filterNotNull()
            .onEach { pointerOffset ->
                if (stateManager.isFocused.value) {
                    pointerInputAwareActors.value.forEach { it.onPointerMove(pointerOffset) }
                }
            }
            .launchIn(scope)
        isPointerPressed
            .onEach { isPointerPressed ->
                pointerOffset.value?.let { pointerOffset ->
                    if (stateManager.isFocused.value) {
                        if (isPointerPressed) {
                            pointerInputAwareActors.value.forEach { it.onPointerPress(pointerOffset) }
                        } else {
                            pointerInputAwareActors.value.forEach { it.onPointerReleased(pointerOffset) }
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    // TODO: Offset issues with fixed aspect ratio
    @Composable
    override fun getOverlayModifier() = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                event.changes.first().position.let { position ->
                    when (event.type) {
                        PointerEventType.Move -> {
                            pointerOffset.value = position
                        }

                        PointerEventType.Press -> {
                            pointerOffset.value = position
                            isPointerPressed.value = true
                        }

                        PointerEventType.Release -> {
                            pointerOffset.value = position
                            isPointerPressed.value = false
                        }
                    }
                }
            }
        }
    }
}