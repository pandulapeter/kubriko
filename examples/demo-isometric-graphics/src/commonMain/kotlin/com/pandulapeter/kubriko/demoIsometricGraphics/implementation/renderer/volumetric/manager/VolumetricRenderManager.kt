/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.actor.RenderableCuboidHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility.rotateAround
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.actor.VolumetricCuboidBatchRenderer
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.actor.VolumetricCuboidRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.math.exp
import kotlin.text.compareTo
import kotlin.text.contains

class VolumetricRenderManager(
    private val allActors: Flow<List<Actor>>,
    private val cameraOffset: StateFlow<SceneOffset>,
) : Manager() {

    private val actorManager by manager<ActorManager>()
    private val viewportManager by manager<ViewportManager>()
    private val _worldRotation = MutableStateFlow(AngleRadians.Zero)
    val worldRotation = _worldRotation.asStateFlow()
    private val _zoom = MutableStateFlow(0.25f)
    val zoom = _zoom.asStateFlow()
    private val _tilt = MutableStateFlow(1f)
    val tilt = _tilt.asStateFlow()
    private val trackedRenderers = MutableStateFlow<Map<String, VolumetricCuboidRenderer>>(emptyMap())
    private var actorsJob: Job? = null

    // All cuboids draw through this single actor (see VolumetricCuboidBatchRenderer); individual
    // renderers are never added to the ActorManager.
    private val batchRenderer = VolumetricCuboidBatchRenderer { sortedRenderers }

    // Tick-thread-private mirror of trackedRenderers.value, kept sorted by drawingOrder (largest
    // first — farthest from the camera — matching the engine's painter's order). Refilled when the
    // published map reference changes, re-sorted only when some renderer actually changed.
    private val sortedRenderers = ArrayList<VolumetricCuboidRenderer>()
    private var lastMirroredRenderers: Map<String, VolumetricCuboidRenderer>? = null

    // Only touched from the diff below, which runs sequentially on Dispatchers.Default.
    private var lastHolders: List<RenderableCuboidHolder> = emptyList()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(batchRenderer)
        actorsJob?.cancel()
        actorsJob = allActors
            .conflate()
            .onEach { actors ->
                val holders = actors.filterIsInstance<RenderableCuboidHolder>()
                // The actor list can change without affecting the holder subset; skip the diff then.
                if (holders == lastHolders) return@onEach
                lastHolders = holders
                val holderIds = holders.mapTo(HashSet()) { it.id }
                val currentRenderers = trackedRenderers.value
                val workingMap = currentRenderers.toMutableMap()
                val iterator = workingMap.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().key !in holderIds) {
                        iterator.remove()
                    }
                }
                holders.forEach { holder ->
                    if (!workingMap.containsKey(holder.id)) {
                        workingMap[holder.id] = VolumetricCuboidRenderer(
                            id = holder.id,
                            renderableCuboid = holder.renderableCuboid,
                            textureResolver = holder.textureResolver,
                            initialWorldRotation = worldRotation.value,
                            initialWorldZoom = zoom.value,
                            initialWorldTilt = tilt.value,
                        )
                    }
                }
                trackedRenderers.value = workingMap
            }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        val currentRotation = worldRotation.value
        val currentZoom = zoom.value
        val currentTilt = tilt.value
        val currentCameraOffset = cameraOffset.value

        val sqrtTwo = 1.4142135f
        val tileWidthMultiplier = currentZoom * sqrtTwo * 2f
        val tileHeightMultiplier = currentZoom * sqrtTwo * currentTilt
        val depthEffect = (tileHeightMultiplier / tileWidthMultiplier) * 1.5f
        val rotatedOffset = currentCameraOffset.rotateAround(SceneOffset.Zero, currentRotation)
        val isometricCameraOffset = SceneOffset(
            x = (rotatedOffset.x - rotatedOffset.y) * tileWidthMultiplier * 0.5f,
            y = (rotatedOffset.x + rotatedOffset.y) * tileHeightMultiplier * 0.5f - ((FOCUS_HEIGHT / depthEffect) * tileHeightMultiplier).sceneUnit,
        )
        viewportManager.setCameraPosition(isometricCameraOffset)

        val currentTrackedRenderers = trackedRenderers.value
        var needsSort = false
        if (currentTrackedRenderers !== lastMirroredRenderers) {
            lastMirroredRenderers = currentTrackedRenderers
            sortedRenderers.clear()
            sortedRenderers.addAll(currentTrackedRenderers.values)
            needsSort = true
        }
        for (i in sortedRenderers.indices) {
            if (sortedRenderers[i].update(
                    worldRotation = currentRotation,
                    worldZoom = currentZoom,
                    worldTilt = currentTilt,
                )
            ) {
                needsSort = true
            }
        }
        // Nearly sorted in the common case (only a few drawingOrders move per frame), which the
        // adaptive merge sort handles in close to linear time. Fully static frames skip it.
        if (needsSort) {
            sortedRenderers.sortWith(drawingOrderComparator)
        }
    }

    fun addToWorldRotation(angleDelta: AngleRadians) = _worldRotation.update { currentAngle -> currentAngle + angleDelta + angleDelta * tilt.value * 0.75f }

    fun addToWorldZoom(zoomDelta: Float) = _zoom.update { currentZoom -> (currentZoom * exp(zoomDelta)).coerceIn(0.1f, 1f) }

    fun multiplyWorldZoom(zoom: Float) = _zoom.update { (it * zoom).coerceIn(0.1f, 1f) }

    fun addToWorldTilt(tiltDelta: Float) = _tilt.update { currentTilt -> (currentTilt * exp(tiltDelta)).coerceIn(0.1f, 2f) }

    companion object {
        const val FOCUS_HEIGHT = 137f

        // +0f normalizes -0.0f so the comparator stays transitive (see the engine's comparator).
        private val drawingOrderComparator = Comparator<VolumetricCuboidRenderer> { a, b ->
            (b.drawingOrder + 0f).compareTo(a.drawingOrder + 0f)
        }
    }
}
