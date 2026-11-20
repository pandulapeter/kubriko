/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.scenes.ActorValueChanges
import com.pandulapeter.kubrikoCreator.commands.scenes.MoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.PlaceActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActor
import com.pandulapeter.kubrikoCreator.commands.scenes.RemoveActors
import com.pandulapeter.kubrikoCreator.commands.scenes.RestoreActors
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsValues
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument

/**
 * The changes the Scene editor makes to the Scene called [sceneName], each as a command through [onCommand], keeping
 * [selection] in step: placed and duplicated Actors are selected, and removed ones no longer.
 */
internal class SceneEditorActions(
    private val sceneName: String,
    private val view: SceneView,
    private val selection: SceneSelection,
    private val onCommand: (Invocation<*>) -> Unit,
) {
    /**
     * Moves each Actor of [moves] to where it says, as one step.
     */
    fun move(moves: List<ActorMove>) {
        val single = moves.singleOrNull()
        when {
            single != null -> onCommand(Invocation(MoveActor, MoveActor.Input(sceneName, single.actorId, single.x, single.y)))
            moves.isNotEmpty() -> onCommand(Invocation(SetActorsValues, SetActorsValues.Input(sceneName, moves.map { ActorValueChanges(it.actorId, x = it.x, y = it.y) })))
        }
    }

    fun transform(actorId: String, handle: FrameHandle, after: ActorFrame) = onCommand(Invocation(SetActorsValues, transformInputOf(sceneName, actorId, handle, after)))

    fun place(actorTypeName: String, x: Float, y: Float) {
        val actorId = SceneDocument.newId(view.entries.map { it.id })
        selection.select(actorId)
        onCommand(Invocation(PlaceActor, PlaceActor.Input(sceneName, actorTypeName, x, y, actorId)))
    }

    /**
     * Places an Actor of [actorTypeName] where the camera looks, on the nearest crossing of [grid] if there is one.
     */
    fun placeInView(actorTypeName: String, grid: SceneGrid?) {
        val camera = view.viewportManager.cameraPosition.value.let { Offset(it.x.raw, it.y.raw) }
        val position = grid?.snap(camera) ?: camera
        place(actorTypeName, position.x, position.y)
    }

    fun remove(actorId: String) = remove(listOf(actorId))

    /**
     * Removes the Actors [actorIds] the Scene still has, as one step.
     */
    fun remove(actorIds: List<String>) {
        val existing = actorIds.filter { view.entryOf(it) != null }
        if (existing.isEmpty()) return
        selection.select(null)
        val single = existing.singleOrNull()
        if (single != null) {
            onCommand(Invocation(RemoveActor, RemoveActor.Input(sceneName, single)))
        } else {
            onCommand(Invocation(RemoveActors, RemoveActors.Input(sceneName, existing)))
        }
    }

    /**
     * Copies the Actors [actorIds] the Scene draws to the end of the Scene, as one step, each moved by [offset] scene
     * units right and down, and selects the copies.
     */
    fun duplicate(actorIds: List<String>, offset: Float) {
        val copies = duplicatesOf(view.entries, actorIds.filter { view.boundsOf(it) != null && !view.isBackground(it) }.toSet(), offset)
        if (copies.isEmpty()) return
        selection.select(copies.mapNotNull { it.actorId })
        onCommand(Invocation(RestoreActors, RestoreActors.Input(sceneName, copies)))
    }
}

/**
 * Where the Scene editor moves an Actor to, in scene units.
 */
internal data class ActorMove(val actorId: String, val x: Float, val y: Float)

/**
 * Copies of the Actors of [entries] whose IDs are among [actorIds], in the Scene's order, to be put in at its end with new
 * IDs, each moved by [offset] right and down if its state saves a position.
 */
internal fun duplicatesOf(entries: List<SceneActorEntry>, actorIds: Set<String>, offset: Float): List<RestoreActors.Restored> {
    val ids = entries.mapTo(HashSet()) { it.id }
    return entries.filter { it.id in actorIds }.mapIndexed { index, entry ->
        val id = SceneDocument.newId(ids).also(ids::add)
        val state = SceneDocument.positionOf(entry.state)?.let { (x, y) -> SceneDocument.withPosition(entry.state, x + offset, y + offset) } ?: entry.state
        RestoreActors.Restored(actorIndex = entries.size + index, typeId = entry.typeId, state = state, actorId = id)
    }
}
