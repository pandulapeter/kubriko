/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoCreator.actors.SceneSettingsCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException

/**
 * A Scene drawn by the project's own Actor classes, in a Kubriko instance of Kubriko Creator's that never runs the game:
 * its Actors are created from the Scene's states as the game creates them, and are only ever moved while they are
 * dragged. Changes go through commands, which change the file, which is [load]ed again.
 */
internal class SceneView(entryPoint: CreatorEntryPoint) {
    private val serializationManager = entryPoint.createSerializationManager()
    val kubriko = Kubriko.newInstance(
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Dynamic,
            minimumScaleFactor = MINIMUM_SCALE_FACTOR,
            maximumScaleFactor = MAXIMUM_SCALE_FACTOR,
        ),
        StateManager.newInstance(shouldAutoStart = false),
        // Actor types may look these up, as they do in the game.
        KeyboardInputManager.newInstance(),
        PointerInputManager.newInstance(),
        serializationManager,
        *entryPoint.createCustomManagers().toTypedArray(),
        instanceNameForLogging = "SceneView",
    )
    val viewportManager = kubriko.get<ViewportManager>()
    private val actorManager = kubriko.get<ActorManager>()

    /**
     * The Scene's entries, and the Actors created from them at the same indices (null where a state can't be read).
     */
    var entries: List<SceneActorEntry> = emptyList()
        private set
    var actors: List<Editable<*>?> = emptyList()
        private set

    /**
     * Changes every time the Actors are created again, for what shows them.
     */
    private val _loadRevision = MutableStateFlow(0)
    val loadRevision = _loadRevision.asStateFlow()

    fun load(content: String) {
        entries = try {
            SceneDocument.parse(content)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
        actors = entries.map(::createActor)
        actorManager.removeAll()
        actorManager.add(actors.filterNotNull())
        _loadRevision.update { it + 1 }
    }

    /**
     * The ID of the Actor drawn on top at [screenOffset], in the viewport's pixels.
     */
    fun actorIdAt(screenOffset: Offset): String? {
        val position = toScene(screenOffset)
        return SceneHitTest.pick(allBounds(), position.x.raw, position.y.raw)?.let { entries[it].id }
    }

    fun entryOf(id: String) = entries.firstOrNull { it.id == id }

    fun actorOf(id: String) = actors.getOrNull(entries.indexOfFirst { it.id == id })

    /**
     * Whether the Actor [id] is the Scene settings, which fill the Scene's bounds behind everything else.
     */
    fun isBackground(id: String) = entryOf(id)?.typeId == SceneSettingsCodeGenerator.SETTINGS_NAME

    fun boundsOf(id: String) = entries.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(::boundsAt)

    private fun boundsAt(index: Int) = actors.getOrNull(index)?.let { boundsOf(index, it, entries[index].typeId == SceneSettingsCodeGenerator.SETTINGS_NAME) }

    /**
     * Moves and zooms the camera to show the Scene's bounds, or without Scene settings, every Actor.
     */
    fun zoomToFit() {
        val size = viewportManager.size.value
        val framing = SceneFraming.fit(allBounds(), size.width, size.height) ?: return
        viewportManager.setCameraPosition(SceneOffset(framing.centerX.sceneUnit, framing.centerY.sceneUnit))
        // The aspect ratio mode may scale the factor that is set further, which the target has to make up for.
        val adjustment = viewportManager.scaleFactor.value.horizontal / viewportManager.rawScaleFactor.value.horizontal
        viewportManager.setScaleFactor(framing.scaleFactor / adjustment)
    }

    private fun allBounds() = actors.indices.mapNotNull(::boundsAt)

    fun toScene(screenOffset: Offset): SceneOffset = screenOffset.toSceneOffset(viewportManager)

    /**
     * Where [position] is in the viewport, in pixels: the inverse of [toScene].
     */
    fun toScreen(position: SceneOffset): Offset {
        val origin = toScene(Offset.Zero)
        val unit = toScene(Offset(1f, 1f)) - origin
        return Offset((position.x - origin.x).raw / unit.x.raw, (position.y - origin.y).raw / unit.y.raw)
    }

    fun dispose() = kubriko.dispose()

    private fun createActor(entry: SceneActorEntry): Editable<*>? = try {
        serializationManager.deserializeActors(SceneDocument.format(listOf(entry))).firstOrNull()
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val MINIMUM_SCALE_FACTOR = 0.1f
        private const val MAXIMUM_SCALE_FACTOR = 10f

        /**
         * The rectangle [actor] covers, or for one without a size, a small square around its position.
         */
        fun boundsOf(index: Int, actor: Editable<*>, isBackground: Boolean = false): SceneHitTest.Bounds {
            val body = actor.body
            val drawingOrder = (actor as? Visible)?.drawingOrder ?: 0f
            return if (body is BoxBody) {
                val box = body.axisAlignedBoundingBox
                SceneHitTest.Bounds(index, box.min.x.raw, box.min.y.raw, box.max.x.raw, box.max.y.raw, drawingOrder, isBackground)
            } else {
                val (x, y) = body.position.x.raw to body.position.y.raw
                SceneHitTest.Bounds(index, x - POINT_SIZE, y - POINT_SIZE, x + POINT_SIZE, y + POINT_SIZE, drawingOrder, isBackground)
            }
        }

        private const val POINT_SIZE = 8f
    }
}
