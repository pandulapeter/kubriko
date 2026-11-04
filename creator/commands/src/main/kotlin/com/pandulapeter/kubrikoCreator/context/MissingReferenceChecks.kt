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

/**
 * Blocks, Events, placeholders, and parents naming an Actor type, a Scene, a Property, or an audio file that doesn't
 * exist: what is left of a thing deleted outside Kubriko Creator (a file deleted in a terminal, a git checkout), since
 * deleting it in Kubriko Creator cleans those up. Each finding is about the type holding the reference, at its place.
 */
internal object MissingReferenceChecks {

    /**
     * @param audioFiles The project's sounds and music, or null if they aren't known, which leaves the play Blocks out.
     */
    fun of(types: List<BlockTypeDefinition>, relations: ProjectRelations, sceneNames: Collection<String>, audioFiles: AudioFiles? = null): List<HealthFinding> {
        val typeNames = types.mapTo(HashSet()) { it.name }
        val declared = types.associate { type -> type.name to type.properties.mapTo(HashSet()) { it.name } }
        val scenes = sceneNames.toHashSet()
        val missingRelations = relations.all.filter { relation ->
            when (val subject = relation.subject) {
                is RelationSubject.Type -> relation.kind != RelationKind.PLACES && relation.kind != RelationKind.NAMES_IN_KOTLIN && subject.name !in typeNames
                is RelationSubject.Scene -> subject.name !in scenes
                is RelationSubject.Property -> declared[subject.owner]?.contains(subject.name) != true
            }
        }.map { finding(it.place, it.subject.toString()) }
        return (missingRelations + audioFiles?.let { missingAudioFiles(types, it) }.orEmpty()).distinct()
    }

    private fun missingAudioFiles(types: List<BlockTypeDefinition>, audioFiles: AudioFiles) = buildList {
        types.forEach { type ->
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    val missing = when (block) {
                        is BlockDefinition.PlaySound -> block.sound.takeIf { it.isNotEmpty() && it !in audioFiles.sounds }
                        is BlockDefinition.PlayMusic -> block.music.takeIf { it.isNotEmpty() && it !in audioFiles.music }
                        else -> null
                    }
                    if (missing != null) add(finding(RelationPlace.Type(type.name, eventIndex, blockIndex), missing))
                }
            }
        }
    }

    private fun finding(place: RelationPlace, missing: String) = HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, place.owner, detail = place.toString(), usage = missing)
}
