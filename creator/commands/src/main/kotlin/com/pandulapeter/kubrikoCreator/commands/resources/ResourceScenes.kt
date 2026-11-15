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

import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.actorTypes.audioReferenceProperties
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState

/**
 * The text each Scene of [sceneNames] naming the sound or music at [oldPath] (from `composeResources`) would have naming
 * [newPath] instead (empty for none), by the Scene's name: as its music, or as what it saves for a sound or music
 * Property; Scenes that name it nowhere, or can't be read, are left out. Blocks.
 */
internal fun CommandContext.scenesWithAudioReplaced(sceneNames: Collection<String>, oldPath: String, newPath: String): Map<String, String> =
    scenesWithAudioReplaced(sceneNames, mapOf(oldPath to newPath))

/**
 * The text each Scene of [sceneNames] naming the sound or music at one of [renames]' keys would have naming its value
 * instead, as [scenesWithAudioReplaced] does for one. Blocks.
 */
internal fun CommandContext.scenesWithAudioReplaced(sceneNames: Collection<String>, renames: Map<String, String>): Map<String, String> {
    val references = audioReferenceProperties(blockTypes?.types?.value.orEmpty())
    // Without a sound or music Property, a Scene names nothing but its music.
    if (references.isEmpty() && renames.keys.none { ResourceKind.of(it) == ResourceKind.MUSIC }) return emptyMap()
    return sceneNames.mapNotNull { name ->
        val actors = scenes.actors(name) ?: return@mapNotNull null
        val settingsIndex = SceneSettingsState.indexIn(actors)
        val changed = actors.mapIndexed { index, actor ->
            if (index == settingsIndex) {
                renames[SceneSettingsState.read(actor.state).music]?.let { actor.copy(state = SceneSettingsState.withMusic(actor.state, it)) } ?: actor
            } else {
                val names = references[actor.typeId] ?: return@mapIndexed actor
                renames.entries.fold(actor) { renamed, (oldPath, newPath) ->
                    SceneDocument.withReferencesReplaced(renamed.state, names, oldPath, newPath)?.let { renamed.copy(state = it) } ?: renamed
                }
            }
        }
        if (changed == actors) null else name to SceneDocument.format(changed)
    }.toMap()
}
