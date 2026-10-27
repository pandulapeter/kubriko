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
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_invalid
import kubriko.creator.generated.resources.error_command_scene_missing

internal fun CommandContext.validateScene(name: String): Rejection? {
    val content = scenes.read(name) ?: return Rejection(Res.string.error_command_scene_missing, name)
    return if (SceneDocument.isValid(content)) null else Rejection(Res.string.error_command_scene_invalid, name)
}

internal fun CommandContext.validateSceneActor(sceneName: String, actorIndex: Int): Rejection? = validateScene(sceneName)
    ?: if (actorIndex !in readScene(sceneName).indices) Rejection(Res.string.error_command_scene_actor_missing, sceneName, actorIndex.toString()) else null

/**
 * The Actors of a Scene that the command has validated.
 */
internal fun CommandContext.readScene(name: String) = SceneDocument.parse(checkNotNull(scenes.read(name)))

@OptIn(EditorMutation::class)
internal suspend fun CommandContext.writeScene(name: String, actors: List<SceneActorEntry>) {
    withContext(ioDispatcher) { scenes.write(name, SceneDocument.format(actors)) }
    onScenesChanged(listOf(name))
}
