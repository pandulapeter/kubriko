/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.sheetOf

/**
 * What [ActorFamilyCodeGenerator] generates the members of each family of a project from, built once per family the first
 * time one of its members is generated, so that generating every type of a project costs as much as its families' sizes
 * add up to, rather than their squares. Made for one regeneration on one thread, so it isn't safe to share between threads.
 *
 * @param actorTypes Every Actor type of the project.
 * @param managerTypes Every Manager type of the project, whose Properties the Blocks may use.
 * @param collidableActorTypeNames The Actor types that get a collision mask ([PluginManagers.collidableActorTypeNames]).
 * @param family The families of [actorTypes].
 * @param spriteSheets How the project's sprites are cut into frames, by their names.
 */
internal class GeneratedFamilies(
    actorTypes: List<ActorTypeDefinition>,
    managerTypes: List<ManagerTypeDefinition>,
    private val collidableActorTypeNames: Set<String>,
    val family: ActorTypeFamily,
    val spriteSheets: Map<String, SpriteSheet> = emptyMap(),
) {
    private val byName by lazy(LazyThreadSafetyMode.NONE) { actorTypes.associateBy { it.name } }
    private val types by lazy(LazyThreadSafetyMode.NONE) { actorTypes + managerTypes }
    private val generated = HashMap<String, GeneratedFamily>()

    /**
     * Every Manager type's actorAdded and actorRemoved Events, which every Actor type's features read.
     */
    val watches by lazy(LazyThreadSafetyMode.NONE) { managerTypes.flatMap { ActorWatch.of(it, actorTypeNames) } }

    /**
     * The Actor types the actorCount conditions count, which `GeneratedScenes` keeps the counts of.
     */
    val countedTypes by lazy(LazyThreadSafetyMode.NONE) { ActorCountCode.countedTypesOf(types, actorTypeNames) }
    private val actorTypeNames by lazy(LazyThreadSafetyMode.NONE) { actorTypes.map { it.name } }

    /**
     * The family [name] belongs to.
     */
    fun of(name: String): GeneratedFamily {
        val root = family.rootOf(name)
        return generated.getOrPut(root) { build(root) }
    }

    private fun build(root: String): GeneratedFamily {
        val members = family.familyOf(root).mapNotNull(byName::get)
        val isAnimated = members.any { spriteSheets.sheetOf(it) != null }
        val scopes = BlockScope.ofFamily(members, types, family).mapValues { (_, scope) -> scope.copy(isAnimated = isAnimated) }
        val rootScope = scopes.getValue(root)
        val features = ActorTypeFeatures(
            events = members.flatMap { family.eventsOf(it.name) },
            scope = rootScope,
            shapes = members.mapTo(mutableSetOf()) { it.shape },
            hasSprite = members.any { it.sprite?.let(SpriteFiles::accessorOf) != null },
            isAnimated = isAnimated,
            isCollidableAnyway = members.any { it.name in collidableActorTypeNames },
            containment = SceneContainment.of(members),
            isFamily = ActorClassEmitter.Role.FAMILY_ROOT.isFamily,
            projectWatches = watches,
            projectCountedTypes = countedTypes,
            eventScopes = members.map { it.events to scopes.getValue(it.name) },
        )
        return GeneratedFamily(root, rootScope, features, scopes)
    }

    /**
     * One family of Actor types, from its topmost type [root] down.
     *
     * @param scopes The scope of each member, by its name.
     */
    class GeneratedFamily(
        val root: String,
        val rootScope: BlockScope,
        val features: ActorTypeFeatures,
        private val scopes: Map<String, BlockScope>,
    ) {
        /**
         * The scope the Blocks of [name] are generated in, calling the parent's Event through [parentEventCall].
         */
        fun scopeOf(name: String, parentEventCall: String? = null): BlockScope = scopes.getValue(name).copy(parentEventCall = parentEventCall)
    }
}
