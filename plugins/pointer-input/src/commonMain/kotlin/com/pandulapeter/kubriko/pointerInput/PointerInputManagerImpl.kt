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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.implementation.gestureDetector
import com.pandulapeter.kubriko.pointerInput.implementation.isMultiTouchEnabled
import com.pandulapeter.kubriko.pointerInput.implementation.setPointerPosition
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
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
    private val actorManager by manager<ActorManager>()
    private val metadataManager by manager<MetadataManager>()
    private val stateManager by manager<StateManager>()
    private val pointerInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<PointerInputAware>() }.asStateFlow(emptyList())
    }
    private val rootOffset = MutableStateFlow(Offset.Zero)
    private val viewportOffset = MutableStateFlow(Offset.Zero)
    private val _pressedPointerPositions = MutableStateFlow(persistentMapOf<PointerId, Offset>())
    override val pressedPointerPositions by autoInitializingLazy {
        combine(
            _pressedPointerPositions,
            rootOffset,
            viewportOffset,
        ) { unprocessed, rootOffset, viewportOffset ->
            unprocessed.keys.associateWith {
                unprocessed[it]!!.let {
                    if (isActiveAboveViewport) it - viewportOffset + rootOffset else it
                }
            }.toPersistentMap()
        }.asStateFlow(persistentMapOf())
    }
    private val _hoveringPointerPosition = MutableStateFlow<Offset?>(null)
    override val hoveringPointerPosition by autoInitializingLazy {
        combine(
            _hoveringPointerPosition,
            rootOffset,
            viewportOffset,
        ) { rawPointerOffset, rootOffset, viewportOffset ->
            if (isActiveAboveViewport) rawPointerOffset?.let { it - viewportOffset + rootOffset } else rawPointerOffset
        }.asStateFlow(null)
    }
    private var mouseId: PointerId? = null

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { _pressedPointerPositions.update { persistentMapOf() } }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        _pressedPointerPositions.value.let { activePointers ->
            if (activePointers.isNotEmpty()) {
                pointerInputAwareActors.value.forEach { it.handleActivePointers(activePointers) }
            }
        }
    }

    private var densityMultiplier = 1f

    override fun tryToMoveHoveringPointer(offset: Offset): Boolean {
        val before = hoveringPointerPosition.value?.round()
        setPointerPosition(
            platform = metadataManager.platform,
            offset = offset + if (isActiveAboveViewport) viewportOffset.value else viewportOffset.value,
            densityMultiplier = densityMultiplier,
        )
        val after = hoveringPointerPosition.value?.round()
        return before != after
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

    private fun Modifier.pointerInputHandlingModifier() = pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (isInitialized.value) {
                    val allChanges = if (isMultiTouchEnabled) event.changes else event.changes.filter { it.id.value == 0L }
                    val currentPointers = allChanges.associate { it.id to it.pressed }
                    allChanges.forEach { change ->
                        val id = change.id
                        val wasPressed = _pressedPointerPositions.value.containsKey(id)
                        val isPressed = currentPointers[id] == true
                        when {
                            !wasPressed && isPressed -> {
                                if (stateManager.isFocused.value) {
                                    _pressedPointerPositions.update {
                                        it.toMutableMap().apply {
                                            this[change.id] = change.position
                                        }.toPersistentMap()
                                    }
                                    pointerInputAwareActors.value.forEach { it.onPointerPressed(id, change.position) }
                                    if (mouseId == id) {
                                        _hoveringPointerPosition.value = change.position
                                    }
                                }
                            }

                            wasPressed && !isPressed -> {
                                _pressedPointerPositions.update {
                                    it.toMutableMap().apply {
                                        remove(change.id)
                                    }.toPersistentMap()
                                }
                                pointerInputAwareActors.value.forEach { it.onPointerReleased(id, change.position) }
                                if (mouseId == id) {
                                    _hoveringPointerPosition.value = change.position
                                }
                            }

                            wasPressed && isPressed -> {
                                if (stateManager.isFocused.value) {
                                    _pressedPointerPositions.update {
                                        it.toMutableMap().apply {
                                            this[change.id] = change.position
                                        }.toPersistentMap()
                                    }
                                    if (id == mouseId) {
                                        _hoveringPointerPosition.value = change.position
                                    }
                                    pointerInputAwareActors.value.forEach { it.onPointerOffsetChanged(id, change.position) }
                                }
                            }

                            event.type == PointerEventType.Move -> {
                                if (stateManager.isFocused.value) {
                                    mouseId = id
                                    _hoveringPointerPosition.value = change.position
                                    pointerInputAwareActors.value.forEach { it.onPointerOffsetChanged(id, change.position) }
                                }
                            }

                            event.type == PointerEventType.Enter -> {
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerEnteringTheViewport() }
                                }
                            }

                            event.type == PointerEventType.Exit -> {
                                if (stateManager.isFocused.value) {
                                    pointerInputAwareActors.value.forEach { it.onPointerLeavingTheViewport() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.gestureDetector(
        onDragDetected = { dragAmount ->
            if (stateManager.isFocused.value) {
                pointerInputAwareActors.value.forEach { it.onPointerDrag(dragAmount) }
            }
        },
        onZoomDetected = { offset, factor ->
            if (stateManager.isFocused.value) {
                pointerInputAwareActors.value.forEach { it.onPointerZoom(offset, factor) }
            }
        },
    )
}