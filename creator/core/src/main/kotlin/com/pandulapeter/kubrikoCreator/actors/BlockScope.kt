/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * What the Blocks of the block-built type [typeName] can refer to when they are generated: its own Properties, and the
 * other types of the project. References to anything else become comments, so that the generated code always compiles.
 *
 * @param actorTypeNames Every Actor type of the project.
 * @param managerTypes Every Manager type of the project.
 * @param sceneNames Every Scene of the project, or null where they aren't known (the generated code loads Scenes by
 *   name, and reports a missing one while the game runs).
 * @param audioFiles The sounds and music of the project, or null where they aren't known.
 * @param baseClassNames The Actor types with children, whose Actors (and their children's) are instances of a base class.
 * @param ancestorNames The Actor type's ancestors, its parent first.
 * @param parentEventCall How the code of the Event being generated runs its parent's Event, or null where there is none.
 * @param timerNames The Timers the type uses, with those of its whole family for an Actor type, since they share them.
 */
public data class BlockScope(
    val typeName: String,
    val kind: BlockTypeKind = BlockTypeKind.ACTOR,
    val properties: List<PropertyDefinition> = emptyList(),
    val actorTypeNames: List<String> = listOf(typeName),
    val managerTypes: List<ManagerTypeDefinition> = emptyList(),
    val sceneNames: List<String>? = null,
    val audioFiles: AudioFiles? = null,
    val baseClassNames: Set<String> = emptySet(),
    val ancestorNames: List<String> = emptyList(),
    val parentEventCall: String? = null,
    val timerNames: List<String> = emptyList(),
) {
    /**
     * The class Actors of [actorType] are instances of, its children's too: its base class if it has children.
     */
    internal fun classOf(actorType: String) = if (actorType in baseClassNames) actorType + BlockTypeNaming.BASE_SUFFIX else actorType

    /**
     * The Properties of [manager], or of the type itself if it is null (or names the Manager type being generated), or
     * null if there is no such Manager type.
     */
    public fun propertiesOf(manager: String?): List<PropertyDefinition>? = if (isOwn(manager)) properties else managerTypes.firstOrNull { it.name == manager }?.properties

    public fun hasProperty(reference: PropertyReference): Boolean = propertiesOf(reference.manager)?.any { it.name == reference.name && PropertyKind.of(it) == reference.kind } == true

    /**
     * How the generated code reaches the Properties of [manager]: `this` for the type's own, otherwise the field it looks
     * the Manager up in ([managerField]).
     */
    internal fun ownerOf(manager: String?) = if (isOwn(manager)) "this" else managerField(checkNotNull(manager))

    /**
     * The field the generated code looks [manager] up in: `scoreManager` for `Score`. No Property can end with `Manager`
     * ([PropertyNaming]), and no Manager type is named after a lookup every type may have (`ViewportManager`), so it
     * clashes with nothing.
     */
    internal fun managerField(manager: String) = managerFieldOf(manager)

    /**
     * The other Manager types that [kotlinUi] names by their field, which the generated class has to look up.
     */
    internal fun managersNamedIn(kotlinUi: KotlinUi?) = kotlinUi?.let { ui -> managerTypes.map { it.name }.filter { !isOwn(it) && ui.names(managerField(it)) } }.orEmpty()

    /**
     * Whether the placeholder names a Property that exists, of any kind.
     */
    public fun hasProperty(placeholder: PropertyPlaceholders.Placeholder): Boolean = propertyOf(placeholder) != null

    internal fun propertyOf(placeholder: PropertyPlaceholders.Placeholder) = propertiesOf(placeholder.manager)?.firstOrNull { it.name == placeholder.name }

    /**
     * Whether the Properties of [manager] are the type's own: it is null, or names the Manager type being generated.
     */
    internal fun isOwn(manager: String?) = manager == null || (kind == BlockTypeKind.MANAGER && manager == typeName)

    public companion object {

        /**
         * The field generated code, and so a Kotlin Block, reaches the Manager type [manager] through ([managerField]).
         */
        public fun managerFieldOf(manager: String): String = manager.replaceFirstChar { it.lowercase() } + MANAGER_FIELD_SUFFIX

        /**
         * What the field a type looks another Manager type up in ends with, after the Manager type's name.
         */
        internal const val MANAGER_FIELD_SUFFIX = "Manager"

        /**
         * The scope of [definition] in a project with [types], [sceneNames], and [audioFiles], whose Actor types make up
         * [family] (passed in when many scopes of one project are made, since building it costs more than the scope).
         */
        public fun of(
            definition: BlockTypeDefinition,
            types: List<BlockTypeDefinition>,
            sceneNames: List<String>? = null,
            audioFiles: AudioFiles? = null,
            family: ActorTypeFamily = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>()),
        ): BlockScope {
            val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
            val timerNames = if (definition is ActorTypeDefinition) {
                val sharers = timerSharersOf(definition.name, types, family).toSet()
                actorTypes.filter { it.name in sharers }.flatMap { it.timerNames }
            } else {
                definition.timerNames
            }
            return scopeOf(definition, actorTypes.map { it.name }, types.filterIsInstance<ManagerTypeDefinition>(), sceneNames, audioFiles, family, timerNames)
        }

        /**
         * The scope of each of [members], every Actor type of one family of [family], by its name: what [of] makes for each,
         * with what they share worked out once rather than per member.
         */
        internal fun ofFamily(members: List<ActorTypeDefinition>, types: List<BlockTypeDefinition>, family: ActorTypeFamily): Map<String, BlockScope> {
            val actorTypeNames = types.filterIsInstance<ActorTypeDefinition>().map { it.name }
            val managerTypes = types.filterIsInstance<ManagerTypeDefinition>()
            val timerNames = members.flatMap { it.timerNames }
            return members.associate { it.name to scopeOf(it, actorTypeNames, managerTypes, sceneNames = null, audioFiles = null, family, timerNames) }
        }

        private fun scopeOf(
            definition: BlockTypeDefinition,
            actorTypeNames: List<String>,
            managerTypes: List<ManagerTypeDefinition>,
            sceneNames: List<String>?,
            audioFiles: AudioFiles?,
            family: ActorTypeFamily,
            timerNames: List<String>,
        ) = BlockScope(
            typeName = definition.name,
            kind = definition.kind,
            properties = if (definition is ActorTypeDefinition) family.propertiesOf(definition.name).ifEmpty { definition.properties } else definition.properties,
            actorTypeNames = actorTypeNames,
            managerTypes = managerTypes,
            sceneNames = sceneNames,
            audioFiles = audioFiles,
            baseClassNames = family.namesWithChildren,
            ancestorNames = family.ancestorsOf(definition.name),
            timerNames = timerNames.distinct().sorted(),
        )
    }
}
