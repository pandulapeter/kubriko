/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.actors.fixedPath

/**
 * Blocks, Events, placeholders, parents, and Scenes' music naming an Actor type, a Scene, a Property, or an audio file that doesn't
 * exist: what is left of a thing deleted outside Kubriko Creator (a file deleted in a terminal, a git checkout), since
 * deleting it in Kubriko Creator cleans those up. Each finding is about the type holding the reference, at its place.
 */
internal object MissingReferenceChecks {

    /**
     * @param audioFiles The project's sounds and music, or null if they aren't known, which leaves the play Blocks and
     *   the Scenes' music out.
     * @param sceneMusic The music each Scene plays, by the Scene's name ([sceneMusic]).
     * @param sceneSavedReferences What the Scenes save for reference Properties ([SceneCounts.savedReferences]), of which
     *   the sound and music paths are checked.
     */
    fun of(
        types: List<BlockTypeDefinition>,
        relations: ProjectRelations,
        sceneNames: Collection<String>,
        audioFiles: AudioFiles? = null,
        sceneMusic: Map<String, String> = emptyMap(),
        sceneSavedReferences: Map<String, Map<String, Int>> = emptyMap(),
    ): List<HealthFinding> {
        val typeNames = types.mapTo(HashSet()) { it.name }
        val declared = types.associate { type -> type.name to type.properties.mapTo(HashSet()) { it.name } }
        val scenes = sceneNames.toHashSet()
        val missingRelations = relations.all.filter { relation ->
            when (val subject = relation.subject) {
                is RelationSubject.Type -> relation.kind != RelationKind.PLACES && relation.kind != RelationKind.NAMES_IN_KOTLIN && subject.name !in typeNames
                is RelationSubject.Scene -> subject.name !in scenes
                is RelationSubject.Property -> declared[subject.owner]?.contains(subject.name) != true
                is RelationSubject.Page, is RelationSubject.Image, is RelationSubject.Resource, is RelationSubject.Timer, is RelationSubject.Folder -> false
            }
        }.map { finding(it.place, it.subject.toString()) }
        val missingSceneMusic = audioFiles?.let { files ->
            sceneMusic.filterValues { it !in files.music }.map { (scene, music) -> finding(RelationPlace.Scene(scene), music) } +
                sceneSavedReferences.flatMap { (scene, counts) -> counts.keys.filter { !files.has(it) }.map { finding(RelationPlace.Scene(scene), it) } }
        }.orEmpty()
        return (missingRelations + audioFiles?.let { missingAudioFiles(types, it) }.orEmpty() + missingSceneMusic).distinct()
    }

    private fun missingAudioFiles(types: List<BlockTypeDefinition>, audioFiles: AudioFiles) = buildList {
        types.forEach { type ->
            type.properties.forEach { property ->
                val missing = when (property) {
                    is PropertyDefinition.Sound -> property.initialValue.takeIf { it.isNotEmpty() && it !in audioFiles.sounds }
                    is PropertyDefinition.Music -> property.initialValue.takeIf { it.isNotEmpty() && it !in audioFiles.music }
                    else -> null
                }
                if (missing != null) add(finding(RelationPlace.Type(type.name), missing))
            }
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    val missing = when (block) {
                        is BlockDefinition.PlaySound -> block.sound.fixedPath?.takeIf { it !in audioFiles.sounds }
                        is BlockDefinition.PlayMusic -> block.music.fixedPath?.takeIf { it !in audioFiles.music }
                        else -> null
                    }
                    if (missing != null) add(finding(RelationPlace.Type(type.name, eventIndex, blockIndex), missing))
                }
            }
        }
    }

    /**
     * Whether [path], a saved reference value, names a sound or music file the project has, or isn't such a path at all
     * (an Actor type's name, which the relations check).
     */
    private fun AudioFiles.has(path: String) = when (ResourceKind.of(path)) {
        ResourceKind.SOUND -> path in sounds
        ResourceKind.MUSIC -> path in music
        else -> true
    }

    private fun finding(place: RelationPlace, missing: String) = HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, place.owner, detail = place.toString(), usage = missing)
}
