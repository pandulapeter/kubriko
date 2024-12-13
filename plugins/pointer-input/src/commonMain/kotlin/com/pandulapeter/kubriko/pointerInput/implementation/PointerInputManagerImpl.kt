package com.pandulapeter.kubriko.pointerInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val rawPointerOffset = MutableStateFlow<Offset?>(null)
    private val isPointerPressed = MutableStateFlow(false)
    private val rootOffset = MutableStateFlow(Offset.Zero)
    private val viewportOffset = MutableStateFlow(Offset.Zero)
    override val pointerScreenOffset by autoInitializingLazy {
        combine(
            rawPointerOffset,
            rootOffset,
            viewportOffset,
        ) { rawPointerOffset, rootOffset, viewportOffset ->
            rawPointerOffset?.let { it - viewportOffset + rootOffset }
        }.asStateFlow(null)
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
        stateManager = kubriko.require<StateManager>()
        stateManager.isFocused
            .filterNot { it }
            .onEach { isPointerPressed.value = false }
            .launchIn(scope)
        pointerScreenOffset
            .filterNotNull()
            .onEach { pointerOffset ->
                if (stateManager.isFocused.value) {
                    pointerInputAwareActors.value.forEach { it.onPointerOffsetChanged(pointerOffset) }
                }
            }
            .launchIn(scope)
        isPointerPressed
            .onEach { isPointerPressed ->
                pointerScreenOffset.value?.let { pointerOffset ->
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

    @Composable
    override fun getOverlayModifier() = Modifier
        .onGloballyPositioned { coordinates ->
            rootOffset.value = coordinates.positionInRoot()
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.first().position.let { position ->
                        when (event.type) {
                            PointerEventType.Move -> {
                                rawPointerOffset.value = position
                            }

                            PointerEventType.Press -> {
                                rawPointerOffset.value = position
                                isPointerPressed.value = true
                            }

                            PointerEventType.Release -> {
                                rawPointerOffset.value = position
                                isPointerPressed.value = false
                            }
                        }
                    }
                }
            }
        }

    @Composable
    override fun getModifier(layerIndex: Int?) = Modifier.onGloballyPositioned { coordinates ->
        viewportOffset.value = coordinates.positionInRoot()
    }
}