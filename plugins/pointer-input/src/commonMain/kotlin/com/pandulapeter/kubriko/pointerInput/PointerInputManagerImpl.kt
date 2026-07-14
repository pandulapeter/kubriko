/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.pointerInput

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.implementation.SyncStateFlow
import com.pandulapeter.kubriko.pointerInput.implementation.gestureDetector
import com.pandulapeter.kubriko.pointerInput.implementation.isMultiTouchEnabled
import com.pandulapeter.kubriko.pointerInput.implementation.setPointerPosition
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
        actorManager.allActors.map { it.filterIsInstance<PointerInputAware>() }.asStateFlowOnMainThread(emptyList())
    }
    private val rootOffset = MutableStateFlow(Offset.Zero)
    private val viewportOffset = MutableStateFlow(Offset.Zero)
    private val _pressedPointerPositions = MutableStateFlow(persistentMapOf<PointerId, Offset>())
    // Move-driven position updates land here (plain, in-place mutated map) instead of going straight
    // into the persistent _pressedPointerPositions map, which would otherwise pay a structural-sharing
    // put on every raw pointer move event; onUpdate() flushes this once per tick instead.
    private val pendingPositionUpdates = mutableMapOf<PointerId, Offset>()
    override val pressedPointerPositions by autoInitializingLazy {
        val combinedFlow = combine(
            _pressedPointerPositions,
            rootOffset,
            viewportOffset,
        ) { unprocessed, rootOffset, viewportOffset ->
            resolvePressedPointerPositions(unprocessed, rootOffset, viewportOffset)
        }.stateIn(scope, SharingStarted.WhileSubscribed(), persistentMapOf())
        SyncStateFlow(combinedFlow) {
            resolvePressedPointerPositions(_pressedPointerPositions.value, rootOffset.value, viewportOffset.value)
        }
    }
    private val _hoveringPointerPosition = MutableStateFlow<Offset?>(null)
    override val hoveringPointerPosition by autoInitializingLazy {
        val combinedFlow = combine(
            _hoveringPointerPosition,
            rootOffset,
            viewportOffset,
        ) { rawPointerOffset, rootOffset, viewportOffset ->
            resolveHoveringPointerPosition(rawPointerOffset, rootOffset, viewportOffset)
        }.stateIn(scope, SharingStarted.WhileSubscribed(), null)
        SyncStateFlow(combinedFlow) {
            resolveHoveringPointerPosition(_hoveringPointerPosition.value, rootOffset.value, viewportOffset.value)
        }
    }
    private var mouseId: PointerId? = null
    // Pointers pressed since the previous tick. Discrete onPointerPressed/onPointerReleased callbacks
    // fire off-tick and are never lost, but a pointer tapped and released entirely between two ticks
    // (common at low frame rates) would otherwise never appear in the per-tick handleActivePointers map.
    // This latch surfaces such a pointer for exactly one tick. Cleared at the end of every onUpdate.
    private val pointersPressedSinceLastTick = mutableMapOf<PointerId, Offset>()

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach {
                val heldPointers = _pressedPointerPositions.value
                _pressedPointerPositions.update { persistentMapOf() }
                pendingPositionUpdates.clear()
                pointersPressedSinceLastTick.clear()
                heldPointers.forEach { (id, position) ->
                    pointerInputAwareActors.value.forEach { it.onPointerReleased(id, position) }
                }
            }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (pendingPositionUpdates.isNotEmpty()) {
            _pressedPointerPositions.update { it.puttingAll(pendingPositionUpdates) }
            pendingPositionUpdates.clear()
        }
        val pressed = _pressedPointerPositions.value
        val activePointers = if (pointersPressedSinceLastTick.isEmpty()) {
            pressed
        } else {
            // Add back pointers tapped (pressed and released) between two ticks, at their press position,
            // unless they are already held. Allocates a map only on this rare path.
            pressed.builder().apply {
                pointersPressedSinceLastTick.forEach { (id, offset) ->
                    if (!containsKey(id)) {
                        put(id, offset)
                    }
                }
            }.build()
        }
        if (activePointers.isNotEmpty()) {
            pointerInputAwareActors.value.forEach { it.handleActivePointers(activePointers) }
        }
        pointersPressedSinceLastTick.clear()
    }

    private fun resolvePressedPointerPositions(
        unprocessed: PersistentMap<PointerId, Offset>,
        rootOffset: Offset,
        viewportOffset: Offset,
    ): PersistentMap<PointerId, Offset> = unprocessed.keys.associateWith { id ->
        unprocessed[id]!!.let { position ->
            if (isActiveAboveViewport) position - viewportOffset + rootOffset else position
        }
    }.toPersistentMap()

    private fun resolveHoveringPointerPosition(
        rawPointerOffset: Offset?,
        rootOffset: Offset,
        viewportOffset: Offset,
    ): Offset? = if (isActiveAboveViewport) rawPointerOffset?.let { it - viewportOffset + rootOffset } else rawPointerOffset

    private var densityMultiplier = 1f

    override fun tryToMoveHoveringPointer(offset: Offset) = setPointerPosition(
        platform = metadataManager.platform,
        offset = offset + viewportOffset.value,
        densityMultiplier = densityMultiplier,
    )

    @Composable
    override fun processOverlayModifier(modifier: Modifier) = modifier.onGloballyPositioned { coordinates ->
        rootOffset.value = coordinates.positionInRoot()
    }.run {
        if (isActiveAboveViewport) pointerInputHandlingModifier() else this
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>) = modifier.onGloballyPositioned { coordinates ->
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
                    event.changes.forEach { change ->
                        if (!isMultiTouchEnabled && change.id.value != 0L) return@forEach
                        val id = change.id
                        val wasPressed = _pressedPointerPositions.value.containsKey(id)
                        val isPressed = change.pressed
                        when {
                            !wasPressed && isPressed -> {
                                if (stateManager.isFocused.value) {
                                    _pressedPointerPositions.update { it.putting(change.id, change.position) }
                                    pointersPressedSinceLastTick[id] = change.position
                                    pointerInputAwareActors.value.forEach { it.onPointerPressed(id, change.position) }
                                    if (mouseId == id) {
                                        _hoveringPointerPosition.value = change.position
                                    }
                                }
                            }

                            wasPressed && !isPressed -> {
                                _pressedPointerPositions.update { it.removing(change.id) }
                                // Drop a pending move for this id: it just got released, and flushing
                                // it afterwards would resurrect it in _pressedPointerPositions.
                                pendingPositionUpdates.remove(change.id)
                                pointerInputAwareActors.value.forEach { it.onPointerReleased(id, change.position) }
                                if (mouseId == id) {
                                    _hoveringPointerPosition.value = change.position
                                }
                            }

                            wasPressed && isPressed -> {
                                if (stateManager.isFocused.value) {
                                    pendingPositionUpdates[change.id] = change.position
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