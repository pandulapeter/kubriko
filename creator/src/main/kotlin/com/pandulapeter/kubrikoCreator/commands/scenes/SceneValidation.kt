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
import kubriko.creator.generated.resources.error_command_actor_id_taken
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
 * Why [actorId] can't be the ID of a new Actor among [actors]: it isn't an ID, or another Actor has it.
 */
internal fun validateNewActorId(actorId: String, actors: List<SceneActorEntry>): Rejection? = when {
    !SceneDocument.isValidId(actorId) -> Rejection(Res.string.error_command_actor_id_invalid, actorId)
    actors.any { it.id == actorId } -> Rejection(Res.string.error_command_actor_id_taken, actorId)
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
