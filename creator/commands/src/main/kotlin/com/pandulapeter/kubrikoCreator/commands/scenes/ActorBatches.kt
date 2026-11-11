/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.commands.ActorsBroughtBack
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateActorType
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument

/**
 * Why [placements] can't be placed in the Scene [scene].
 */
internal fun CommandContext.validatePlacements(scene: String, placements: List<PlaceActors.Placement>): Rejection? = validateScene(scene)
    ?: validateActorList(placements.map { it.actorId })
    ?: run {
        val ids = readScene(scene).mapTo(HashSet()) { it.id }
        placements.firstNotNullOfOrNull { placement ->
            validateActorType(placement.actorType)
                ?: (if (placement.x.isFinite() && placement.y.isFinite()) null else Rejection(CommandProblem.NUMBER_INVALID))
                ?: placement.actorId?.let { id -> validateNewActorId(id) { it in ids } }
        }
    }

/**
 * Adds the Actors of [placements], which [validatePlacements] accepted, to the end of the Scene [scene].
 *
 * @return The placed Actors, each with its ID: the one given, or a new one.
 */
internal suspend fun CommandContext.placeActors(scene: String, placements: List<PlaceActors.Placement>): List<SceneActorEntry> {
    val actors = readScene(scene)
    val ids = actors.mapTo(HashSet()) { it.id }
    placements.forEach { placement -> placement.actorId?.let(ids::add) }
    val placed = placements.map { placement ->
        val definition = requireActorType(placement.actorType)
        val id = placement.actorId ?: SceneDocument.newId(ids).also(ids::add)
        SceneActorEntry(placement.actorType, SceneDocument.blockBuiltState(placement.x, placement.y, definition.width, definition.height), id)
    }
    writeScene(scene, actors + placed)
    return placed
}

/**
 * Why [changes] can't be made to the Actors of the Scene [scene].
 */
internal fun CommandContext.validateActorChanges(scene: String, changes: List<ActorValueChanges>): Rejection? = validateSceneActors(scene, changes.map { it.actorId })
    ?: run {
        val actors = readScene(scene).associateBy { it.id }
        changes.firstNotNullOfOrNull { change -> validateActorValues(actors.getValue(change.actorId), change) }
    }

/**
 * Makes [changes], which [validateActorChanges] accepted, to the Actors of the Scene [scene].
 *
 * @return The states the changed Actors had, in the order of the Scene.
 */
internal suspend fun CommandContext.changeActorValues(scene: String, changes: List<ActorValueChanges>): List<SetActorsState.ActorState> {
    val actors = readScene(scene)
    val changesById = changes.associateBy { it.actorId }
    val previous = ArrayList<SetActorsState.ActorState>(changesById.size)
    writeScene(
        scene,
        actors.map { actor ->
            changesById[actor.id]?.let { change ->
                previous += SetActorsState.ActorState(actor.id, actor.state)
                actor.copy(state = change.applyTo(actor.state))
            } ?: actor
        },
    )
    return previous
}

/**
 * Why the Actors of the Scene [scene] can't get [states].
 */
internal fun CommandContext.validateStates(scene: String, states: List<SetActorsState.ActorState>): Rejection? = validateSceneActors(scene, states.map { it.actorId })
    ?: if (states.any { SceneDocument.stateObject(it.state) == null }) Rejection(CommandProblem.SCENE_CONTENT_INVALID) else null

/**
 * Gives the Actors of the Scene [scene] [states], which [validateStates] accepted.
 *
 * @return The states they had, in the order of the Scene.
 */
internal suspend fun CommandContext.replaceStates(scene: String, states: List<SetActorsState.ActorState>): List<SetActorsState.ActorState> {
    val actors = readScene(scene)
    val statesById = states.associate { it.actorId to it.state }
    val previous = ArrayList<SetActorsState.ActorState>(statesById.size)
    writeScene(
        scene,
        actors.map { actor ->
            statesById[actor.id]?.let { state ->
                previous += SetActorsState.ActorState(actor.id, actor.state)
                actor.copy(state = state)
            } ?: actor
        },
    )
    return previous
}

/**
 * Why the Actors [actorIds] can't be removed from the Scene [scene].
 */
internal fun CommandContext.validateRemovals(scene: String, actorIds: List<String>): Rejection? = validateSceneActors(scene, actorIds)

/**
 * Removes the Actors [actorIds], which [validateRemovals] accepted, from the Scene [scene].
 *
 * @return The removed Actors as they bring them back, in the order of the Scene.
 */
internal suspend fun CommandContext.removeActors(scene: String, actorIds: List<String>): List<RestoreActors.Restored> {
    val actors = readScene(scene)
    val ids = actorIds.toHashSet()
    val removed = actors.withIndex().filter { it.value.id in ids }
    writeScene(scene, actors.filter { it.id !in ids })
    return removed.map { (index, actor) -> RestoreActors.Restored(index, actor.typeId, actor.state, actor.id) }
}

/**
 * Why [restorations] can't be put into the Scene [scene].
 */
internal fun CommandContext.validateRestorations(scene: String, restorations: List<RestoreActors.Restored>): Rejection? = validateScene(scene)
    ?: validateActorList(restorations.map { it.actorId })
    ?: run {
        val actors = readScene(scene)
        val ids = actors.mapTo(HashSet()) { it.id }
        restorations.withIndex().firstNotNullOfOrNull { (position, restored) ->
            when {
                restored.actorIndex !in 0..actors.size + position -> Rejection(CommandProblem.INDEX_INVALID, restored.actorIndex.toString(), (actors.size + position).toString())
                SceneDocument.stateObject(restored.state) == null -> Rejection(CommandProblem.SCENE_CONTENT_INVALID)
                else -> restored.actorId?.let { id -> validateNewActorId(id) { it in ids } }
            }
        }
    }

/**
 * Puts [restorations], which [validateRestorations] accepted, into the Scene [scene], one after another.
 *
 * @return The Actors put in, in the order given, each with its ID.
 */
internal suspend fun CommandContext.restoreActors(scene: String, restorations: List<RestoreActors.Restored>): ActorsBroughtBack {
    val actors = readScene(scene).toMutableList()
    val ids = actors.mapTo(HashSet()) { it.id }
    restorations.forEach { restored -> restored.actorId?.let(ids::add) }
    val restoredEntries = restorations.map { restored ->
        val id = restored.actorId ?: SceneDocument.newId(ids).also(ids::add)
        val entry = SceneActorEntry(restored.typeId, restored.state, id)
        actors.add(restored.actorIndex, entry)
        entry
    }
    writeScene(scene, actors)
    return ActorsBroughtBack(scene, restoredEntries)
}
