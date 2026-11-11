/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState

/**
 * The text each Scene of [sceneNames] that plays the music at [oldPath] (from `composeResources`) would have playing
 * [newPath] instead (empty for none), by the Scene's name; Scenes that play other music, or can't be read, are left out.
 * Blocks.
 */
internal fun CommandContext.scenesWithMusicReplaced(sceneNames: Collection<String>, oldPath: String, newPath: String): Map<String, String> =
    scenesWithMusicReplaced(sceneNames, mapOf(oldPath to newPath))

/**
 * The text each Scene of [sceneNames] playing the music at one of [renames]' keys would have playing its value
 * instead, as [scenesWithMusicReplaced] does for one. Blocks.
 */
internal fun CommandContext.scenesWithMusicReplaced(sceneNames: Collection<String>, renames: Map<String, String>): Map<String, String> = sceneNames.mapNotNull { name ->
    val actors = scenes.actors(name) ?: return@mapNotNull null
    val index = SceneSettingsState.indexIn(actors).takeIf { it >= 0 } ?: return@mapNotNull null
    val settings = actors[index]
    val newPath = renames[SceneSettingsState.read(settings.state).music] ?: return@mapNotNull null
    name to SceneDocument.format(actors.toMutableList().apply { set(index, settings.copy(state = SceneSettingsState.withMusic(settings.state, newPath))) })
}.toMap()
