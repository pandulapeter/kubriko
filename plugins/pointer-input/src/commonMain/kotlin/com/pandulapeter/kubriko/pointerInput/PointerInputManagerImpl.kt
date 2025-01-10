package com.pandulapeter.kubriko.pointerInput

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.implementation.setPointerPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PointerInputManagerImpl(
    private val isActiveAboveViewport: Boolean,
    isLoggingEnabled: Boolean,
) : PointerInputManager(isLoggingEnabled) {
    // TODO: Implement multi-touch support
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val pointerInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<PointerInputAware>() }.asStateFlow(emptyList())
    }
    private val rawPointerOffset = MutableStateFlow<Offset?>(null)
    override val isPointerPressed = MutableStateFlow(false)
    private val rootOffset = MutableStateFlow(Offset.Zero)
    private val viewportOffset = MutableStateFlow(Offset.Zero)
    override val pointerScreenOffset by autoInitializingLazy {
        combine(
            rawPointerOffset,
            rootOffset,
            viewportOffset,
        ) { rawPointerOffset, rootOffset, viewportOffset ->
            if (isActiveAboveViewport) rawPointerOffset?.let { it - viewportOffset + rootOffset } else rawPointerOffset
        }.asStateFlow(null)
    }

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { isPointerPressed.value = false }
            .launchIn(scope)
    }

    private var densityMultiplier = 1f

    override fun movePointer(offset: Offset) = setPointerPosition(
        offset = offset + if (isActiveAboveViewport) viewportOffset.value else viewportOffset.value,
        densityMultiplier = densityMultiplier,
    )

    @Composable
    override fun processOverlayModifier(modifier: Modifier) = modifier.onGloballyPositioned { coordinates ->
        rootOffset.value = coordinates.positionInRoot()
    }.run {
        if (isActiveAboveViewport) pointerInputHandlingModifier() else this
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = modifier.onGloballyPositioned { coordinates ->
        viewportOffset.value = coordinates.positionInRoot()
    }.run {
        if (isActiveAboveViewport) this else pointerInputHandlingModifier()
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        densityMultiplier = 1 / LocalDensity.current.density
    }

    private fun Modifier.pointerInputHandlingModifier() = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (isInitialized.value) {
                    event.changes.first().position.let { position ->
                        when (event.type) {
                            PointerEventType.Press -> {
                                rawPointerOffset.update { position }
                                isPointerPressed.update { true }
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerPressed(position) }
                                }
                            }

                            PointerEventType.Release -> {
                                rawPointerOffset.update { position }
                                isPointerPressed.update { false }
                                pointerInputAwareActors.value.forEach { it.onPointerReleased(position) }
                            }

                            PointerEventType.Move -> {
                                rawPointerOffset.update { position }
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerOffsetChanged(position) }
                                }
                            }

                            PointerEventType.Exit -> {
                                rawPointerOffset.update { null }
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerExit() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}