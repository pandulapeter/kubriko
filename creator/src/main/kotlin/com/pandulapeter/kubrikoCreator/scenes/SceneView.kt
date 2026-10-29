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
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.helpers.TickSource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * A Scene drawn by the project's own Actor classes, in a Kubriko instance of Kubriko Creator's that never runs the game:
 * its Actors are created from the Scene's states as the game creates them, and are only ever moved while they are
 * dragged. Changes go through commands, which change the Scene, which is [load]ed again, changing only what changed.
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
        // Actors are only drawn once a tick has culled them, and a Scene swapped in while Creator is in the background
        // (its project edited elsewhere) would stay empty until Creator got the focus back.
        tickSource = TickSource.viewportFrames(shouldPauseOnFocusLoss = false),
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
    private var indicesById: Map<String, Int> = emptyMap()

    /**
     * The Actors' bounds for picking, built again on the first pick after a change that added, removed, or reordered
     * Actors, and updated in place for one that only moved or changed some.
     */
    private val boundsGrid = SceneBoundsGrid()
    private var isBoundsGridOutdated = true

    /**
     * Changes every time the Actors are created again, for what shows them.
     */
    private val _loadRevision = MutableStateFlow(0)
    val loadRevision = _loadRevision.asStateFlow()

    /**
     * Shows the Scene's [next] Actors, its new and changed ones created off the main thread. Only what changed
     * since the last [load] changes in the Kubriko instance ([ScenePatch]): a moved Actor is moved, a changed one is
     * created again (so it is drawn after others of its drawing order until the Scene is opened again), and the rest
     * stay, unless their order changed, which creates the whole Scene again.
     */
    suspend fun load(next: List<SceneActorEntry>) {
        val patch = ScenePatch.between(entries, next)
        if (patch == null) {
            val created = withContext(Dispatchers.Default) { next.map(::createActor) }
            entries = next
            actors = created
            isBoundsGridOutdated = true
            actorManager.removeAll()
            actorManager.add(created.filterNotNull())
        } else {
            apply(patch, next)
        }
        indicesById = entries.withIndex().associate { (index, entry) -> entry.id to index }
        _loadRevision.update { it + 1 }
    }

    private suspend fun apply(patch: ScenePatch, next: List<SceneActorEntry>) {
        val created = withContext(Dispatchers.Default) {
            next.indices.filter { patch.actions[it] is ScenePatch.Action.Replace || patch.actions[it] == ScenePatch.Action.Add }.associateWith { createActor(next[it]) }
        }
        val removed = ArrayList<Actor>()
        val added = ArrayList<Actor>()
        val nextActors = next.indices.map { index ->
            when (val action = patch.actions[index]) {
                is ScenePatch.Action.Keep -> actors[action.previousIndex]
                is ScenePatch.Action.Move -> actors[action.previousIndex]?.also { it.body.position = SceneOffset(action.x.sceneUnit, action.y.sceneUnit) }
                is ScenePatch.Action.Replace -> {
                    actors[action.previousIndex]?.let(removed::add)
                    created[index]?.also(added::add)
                }
                ScenePatch.Action.Add -> created[index]?.also(added::add)
            }
        }
        patch.removedIndices.forEach { index -> actors[index]?.let(removed::add) }
        entries = next
        actors = nextActors
        if (patch.removedIndices.isNotEmpty() || ScenePatch.Action.Add in patch.actions) {
            isBoundsGridOutdated = true
        } else if (!isBoundsGridOutdated) {
            patch.actions.forEachIndexed { index, action -> if (action is ScenePatch.Action.Move || action is ScenePatch.Action.Replace) boundsGrid.update(index, boundsAt(index)) }
        }
        if (removed.isNotEmpty()) actorManager.remove(removed)
        if (added.isNotEmpty()) actorManager.add(added)
    }

    /**
     * The ID of the Actor drawn on top at [screenOffset], in the viewport's pixels.
     */
    fun actorIdAt(screenOffset: Offset): String? {
        val position = toScene(screenOffset)
        return currentBoundsGrid().pick(position.x.raw, position.y.raw)?.let { entries[it].id }
    }

    private fun currentBoundsGrid() = boundsGrid.also {
        if (isBoundsGridOutdated) {
            it.rebuild(actors.indices.mapNotNull(::boundsAt), actors.size)
            isBoundsGridOutdated = false
        }
    }

    fun entryOf(id: String) = indicesById[id]?.let(entries::get)

    fun actorOf(id: String) = indicesById[id]?.let(actors::get)

    /**
     * Whether the Actor [id] is the Scene settings, which fill the Scene's bounds behind everything else.
     */
    fun isBackground(id: String) = entryOf(id)?.typeId == SceneSettingsCodeGenerator.SETTINGS_NAME

    fun boundsOf(id: String) = indicesById[id]?.let(::boundsAt)

    private fun boundsAt(index: Int) = actors.getOrNull(index)?.let { boundsOf(index, it, entries[index].typeId == SceneSettingsCodeGenerator.SETTINGS_NAME) }

    /**
     * Moves and zooms the camera to show the Scene's bounds, or without Scene settings, every Actor.
     */
    fun zoomToFit() {
        val size = viewportManager.size.value
        val framing = SceneFraming.fit(currentBoundsGrid().all(), size.width, size.height) ?: return
        viewportManager.setCameraPosition(SceneOffset(framing.centerX.sceneUnit, framing.centerY.sceneUnit))
        // The aspect ratio mode may scale the factor that is set further, which the target has to make up for.
        val adjustment = viewportManager.scaleFactor.value.horizontal / viewportManager.rawScaleFactor.value.horizontal
        viewportManager.setScaleFactor(framing.scaleFactor / adjustment)
    }

    var camera: SceneCamera
        get() = SceneCamera(viewportManager.cameraPosition.value, viewportManager.rawScaleFactor.value.horizontal)
        set(value) {
            viewportManager.setCameraPosition(value.position)
            viewportManager.setScaleFactor(value.scaleFactor)
        }

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
    } catch (_: LinkageError) {
        // Classes compiled from older sources than the ones they use; Reload compiles them all again.
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
