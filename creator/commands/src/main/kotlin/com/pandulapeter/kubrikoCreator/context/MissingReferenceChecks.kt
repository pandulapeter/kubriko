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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition

/**
 * Blocks, Events, placeholders, and parents naming an Actor type, a Scene, or a Property that doesn't exist: what is left
 * of a thing deleted outside Kubriko Creator (a file deleted in a terminal, a git checkout), since deleting it in Kubriko
 * Creator cleans those up.
 */
internal object MissingReferenceChecks {

    fun of(types: List<BlockTypeDefinition>, relations: ProjectRelations, sceneNames: Collection<String>): List<HealthFinding> {
        val typeNames = types.mapTo(HashSet()) { it.name }
        val declared = types.associate { type -> type.name to type.properties.mapTo(HashSet()) { it.name } }
        val scenes = sceneNames.toHashSet()
        return relations.all.filter { relation ->
            when (val subject = relation.subject) {
                is RelationSubject.Type -> relation.kind != RelationKind.PLACES && relation.kind != RelationKind.NAMES_IN_KOTLIN && subject.name !in typeNames
                is RelationSubject.Scene -> subject.name !in scenes
                is RelationSubject.Property -> declared[subject.owner]?.contains(subject.name) != true
            }
        }.map { HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, it.subject, it.place.toString()) }.distinct()
    }
}
