/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.implementation.setPointerPosition
import com.pandulapeter.kubriko.pointerInput.implementation.zoomDetector
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
    instanceNameForLogging: String?,
) : PointerInputManager(isLoggingEnabled, instanceNameForLogging) {
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

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (isPointerPressed.value && stateManager.isFocused.value) {
            pointerScreenOffset.value?.let { pointerScreenOffset ->
                pointerInputAwareActors.value.forEach { it.handleActivePointers(pointerScreenOffset) }
            }
        }
    }

    private var densityMultiplier = 1f

    override fun movePointer(offset: Offset): Boolean {
        val currentOffset = pointerScreenOffset.value
        setPointerPosition(
            offset = offset + if (isActiveAboveViewport) viewportOffset.value else viewportOffset.value,
            densityMultiplier = densityMultiplier,
        )
        return currentOffset != pointerScreenOffset.value
    }

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
    override fun Composable(windowInsets: WindowInsets) {
        densityMultiplier = 1 / LocalDensity.current.density
    }

    // TODO: Hacky solution. Why did we suddenly start getting duplicated events?
    private var previousChange: PointerInputChange? = null

    private fun Modifier.pointerInputHandlingModifier() = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.first()
                if (isInitialized.value && change.uptimeMillis != previousChange?.uptimeMillis) {
                    previousChange = change
                    change.position.let { position ->
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

                            PointerEventType.Enter -> {
                                rawPointerOffset.update { position }
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerEnteringTheViewport() }
                                }
                            }

                            PointerEventType.Exit -> {
                                rawPointerOffset.update { null }
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerLeavingTheViewport() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { _, dragAmount ->
                if (stateManager.isFocused.value) {
                    pointerInputAwareActors.value.forEach { it.onPointerDrag(dragAmount) }
                }
            },
        )
    }.zoomDetector { offset, factor ->
        if (stateManager.isFocused.value) {
            pointerInputAwareActors.value.forEach { it.onPointerZoom(offset, factor) }
        }
    }
}