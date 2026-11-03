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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneSettingsCodeGenerator

/**
 * A map of the game that Kubriko Creator computes from its model, not the AI: its types by folder, each with what it is
 * for, its Scenes, and who adds, collides with, reads, changes, or goes to whom. It opens every conversation of the
 * assistant, and the user sees it as the map of the game.
 */
public data class ProjectDigest(
    val projectName: String,
    val actorTypes: List<Type>,
    val managerTypes: List<Type>,
    val scenes: List<Scene>,
    val relations: List<RelationLine>,
) {
    /**
     * @param folder Its folder, as a path of folder names, empty for none.
     * @param properties Its own Properties, as [TypeNotation] writes them.
     * @param events Its own Events, by title.
     */
    public data class Type(
        val name: String,
        val folder: String,
        val description: String?,
        val parent: String?,
        val properties: List<String>,
        val events: List<String>,
    )

    /**
     * @param actorCounts How many Actors of each type ID it places, its settings left out.
     */
    public data class Scene(
        val name: String,
        val description: String?,
        val actorCounts: Map<String, Int>,
    )

    /**
     * Everything one type refers to in one way: `Ball collides with Brick, Paddle`. What Scenes place is in [Scene].
     *
     * @param subjects What it refers to, each once, in the order first referred to.
     */
    public data class RelationLine(
        val owner: RelationSubject,
        val kind: RelationKind,
        val subjects: List<RelationSubject>,
    )

    public companion object {

        /**
         * @param sceneDescriptions What each Scene is for, by its name.
         */
        public fun of(
            projectName: String,
            types: List<BlockTypeDefinition>,
            sceneCounts: Map<String, Map<String, Int>>,
            sceneDescriptions: Map<String, String>,
        ): ProjectDigest {
            val relations = ProjectRelations(types, sceneCounts)
            val sorted = types.sortedWith(compareBy({ it.folder }, { it.name }))
            return ProjectDigest(
                projectName = projectName,
                actorTypes = sorted.filterIsInstance<ActorTypeDefinition>().map { typeOf(it, it.parent) },
                managerTypes = sorted.filterIsInstance<ManagerTypeDefinition>().map { typeOf(it, parent = null) },
                scenes = sceneCounts.toSortedMap().map { (name, counts) ->
                    Scene(name, sceneDescriptions[name], counts.filterKeys { it != SceneSettingsCodeGenerator.SETTINGS_NAME })
                },
                relations = linesOf(relations.all),
            )
        }

        private fun typeOf(type: BlockTypeDefinition, parent: String?) = Type(
            name = type.name,
            folder = type.folder,
            description = type.description,
            parent = parent,
            properties = type.properties.map(TypeNotation::of),
            events = type.events.map(BlockNotation::of),
        )

        private fun linesOf(relations: List<Relation>) = relations
            .filter { it.kind != RelationKind.INHERITS_FROM && it.kind != RelationKind.PLACES }
            .groupBy { it.place.owner to it.kind }
            .map { (key, grouped) -> RelationLine(owner = key.first, kind = key.second, subjects = grouped.map { it.subject }.distinct()) }
    }
}
