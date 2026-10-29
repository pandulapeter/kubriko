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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_id_invalid
import kubriko.creator.generated.resources.error_command_actor_id_repeated
import kubriko.creator.generated.resources.error_command_actor_id_taken
import kubriko.creator.generated.resources.error_command_actors_empty
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_invalid
import kubriko.creator.generated.resources.error_command_scene_missing

internal fun CommandContext.validateScene(name: String): Rejection? = when {
    !scenes.exists(name) -> Rejection(Res.string.error_command_scene_missing, name)
    scenes.isValid(name) -> null
    else -> Rejection(Res.string.error_command_scene_invalid, name)
}

internal fun CommandContext.validateSceneActor(sceneName: String, actorId: String): Rejection? = validateScene(sceneName)
    ?: if (readScene(sceneName).none { it.id == actorId }) Rejection(Res.string.error_command_scene_actor_missing, sceneName, actorId) else null

/**
 * Why [actorIds] can't all be changed together in the Scene called [sceneName]: there are none, one is given twice, or one isn't there.
 */
internal fun CommandContext.validateSceneActors(sceneName: String, actorIds: List<String>): Rejection? = validateScene(sceneName) ?: validateActorList(actorIds) ?: run {
    val ids = readScene(sceneName).mapTo(HashSet()) { it.id }
    actorIds.firstOrNull { it !in ids }?.let { Rejection(Res.string.error_command_scene_actor_missing, sceneName, it) }
}

/**
 * Why [actorIds], the Actors a batch command names, can't be used: there are none, or one is given twice.
 */
internal fun validateActorList(actorIds: List<String?>): Rejection? {
    if (actorIds.isEmpty()) return Rejection(Res.string.error_command_actors_empty)
    val seen = HashSet<String>()
    return actorIds.firstOrNull { it != null && !seen.add(it) }?.let { Rejection(Res.string.error_command_actor_id_repeated, it) }
}

/**
 * Why [actorId] can't be the ID of a new Actor among [actors]: it isn't an ID, or another Actor has it.
 */
internal fun validateNewActorId(actorId: String, actors: List<SceneActorEntry>): Rejection? = validateNewActorId(actorId) { id -> actors.any { it.id == id } }

/**
 * Why [actorId] can't be the ID of a new Actor in a Scene whose IDs [isTaken] knows.
 */
internal inline fun validateNewActorId(actorId: String, isTaken: (String) -> Boolean): Rejection? = when {
    !SceneDocument.isValidId(actorId) -> Rejection(Res.string.error_command_actor_id_invalid, actorId)
    isTaken(actorId) -> Rejection(Res.string.error_command_actor_id_taken, actorId)
    else -> null
}

/**
 * [this] Scene's Actors with the one called [actorId] changed by [change].
 */
internal fun List<SceneActorEntry>.withActorChanged(actorId: String, change: (SceneActorEntry) -> SceneActorEntry) = map { if (it.id == actorId) change(it) else it }

/**
 * The Actor called [actorId] in a Scene a command has validated it in.
 */
internal fun List<SceneActorEntry>.requireActor(actorId: String) = first { it.id == actorId }

/**
 * The Actors of a Scene that the command has validated.
 */
internal fun CommandContext.readScene(name: String) = checkNotNull(scenes.actors(name))

@OptIn(EditorMutation::class)
internal suspend fun CommandContext.writeScene(name: String, actors: List<SceneActorEntry>) {
    withContext(ioDispatcher) { scenes.write(name, actors) }
    onScenesChanged(listOf(name))
}
