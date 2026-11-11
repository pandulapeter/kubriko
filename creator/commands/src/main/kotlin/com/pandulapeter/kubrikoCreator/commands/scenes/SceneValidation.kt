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

import com.pandulapeter.kubrikoCreator.actors.NameFlaw
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext

internal fun CommandContext.validateScene(name: String): Rejection? = when {
    !scenes.exists(name) -> missingScene(name)
    scenes.isValid(name) -> null
    else -> Rejection(CommandProblem.SCENE_INVALID, name)
}

internal fun CommandContext.validateSceneActor(sceneName: String, actorId: String): Rejection? = validateScene(sceneName)
    ?: readScene(sceneName).let { actors -> if (actors.none { it.id == actorId }) missingActor(sceneName, actorId, actors) else null }

/**
 * Why [actorIds] can't all be changed together in the Scene called [sceneName]: there are none, one is given twice, or one isn't there.
 */
internal fun CommandContext.validateSceneActors(sceneName: String, actorIds: List<String>): Rejection? = validateScene(sceneName) ?: validateActorList(actorIds) ?: run {
    val actors = readScene(sceneName)
    val ids = actors.mapTo(HashSet()) { it.id }
    actorIds.firstOrNull { it !in ids }?.let { missingActor(sceneName, it, actors) }
}

/**
 * Why there is no Scene called [name], naming the Scenes called similarly.
 */
internal fun CommandContext.missingScene(name: String) = SimilarNames.rejection(LookupProblem.SCENE_MISSING, LookupProblem.SCENE_MISSING_SIMILAR, name, scenes.names(), name)

private fun missingActor(sceneName: String, actorId: String, actors: List<SceneActorEntry>) =
    SimilarNames.rejection(LookupProblem.SCENE_ACTOR_MISSING, LookupProblem.SCENE_ACTOR_MISSING_SIMILAR, actorId, actors.map { it.id }, sceneName, actorId)

/**
 * Why [actorIds], the Actors a batch command names, can't be used: there are none, or one is given twice.
 */
internal fun validateActorList(actorIds: List<String?>): Rejection? {
    if (actorIds.isEmpty()) return Rejection(CommandProblem.ACTORS_EMPTY)
    val seen = HashSet<String>()
    return actorIds.firstOrNull { it != null && !seen.add(it) }?.let { Rejection(CommandProblem.ACTOR_ID_REPEATED, it) }
}

/**
 * Why [actorId] can't be the ID of a new Actor among [actors]: it isn't an ID, or another Actor has it.
 */
internal fun validateNewActorId(actorId: String, actors: List<SceneActorEntry>): Rejection? = validateNewActorId(actorId) { id -> actors.any { it.id == id } }

/**
 * Why [actorId] can't be the ID of a new Actor in a Scene whose IDs [isTaken] knows.
 */
internal inline fun validateNewActorId(actorId: String, isTaken: (String) -> Boolean): Rejection? = when {
    actorId.isEmpty() -> Rejection(CommandProblem.ACTOR_ID_EMPTY)
    NameFlaw.hasLetterBeyondAToZ(actorId) -> Rejection(CommandProblem.ACTOR_ID_LETTER_BEYOND_A_TO_Z, actorId)
    !SceneDocument.isValidId(actorId) -> Rejection(CommandProblem.ACTOR_ID_INVALID, actorId)
    isTaken(actorId) -> Rejection(CommandProblem.ACTOR_ID_TAKEN, actorId)
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
}
