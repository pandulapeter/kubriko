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
 */
internal data class BlockScope(
    val typeName: String,
    val kind: BlockTypeKind = BlockTypeKind.ACTOR,
    val properties: List<PropertyDefinition> = emptyList(),
    val actorTypeNames: List<String> = listOf(typeName),
    val managerTypes: List<ManagerTypeDefinition> = emptyList(),
    val sceneNames: List<String>? = null,
) {
    /**
     * The Properties of [manager], or of the type itself if it is null (or names the Manager type being generated), or
     * null if there is no such Manager type.
     */
    fun propertiesOf(manager: String?) = if (isOwn(manager)) properties else managerTypes.firstOrNull { it.name == manager }?.properties

    fun hasProperty(reference: PropertyReference) = propertiesOf(reference.manager)?.any { it.name == reference.name && PropertyKind.of(it) == reference.kind } == true

    /**
     * How the generated code reaches the Properties of [manager]: `this` for the type's own, otherwise a field named
     * after the Manager type, unless one of the type's Properties is called that already.
     */
    fun ownerOf(manager: String?) = if (isOwn(manager)) "this" else managerField(checkNotNull(manager))

    fun managerField(manager: String) = manager.replaceFirstChar { it.lowercase() }.let { field -> if (properties.any { it.name == field }) field + "Manager" else field }

    /**
     * The other Manager types that [blocks] and [texts] use, which the generated class has to look up.
     */
    fun managersUsedBy(blocks: List<BlockDefinition>, texts: List<String> = emptyList()) = (
        blocks.mapNotNull { block -> block.propertyReference?.takeIf { it.name.isNotEmpty() && hasProperty(it) }?.manager } +
            (blocks.mapNotNull { (it as? BlockDefinition.Log)?.message } + texts).flatMap { text ->
                PropertyPlaceholders.find(text).filter { hasProperty(it) }.mapNotNull { it.manager }.toList()
            }
        ).filterNot { isOwn(it) }.distinct().sorted()

    /**
     * The other Manager types that [kotlinUi] names by their field, which the generated class has to look up.
     */
    fun managersNamedIn(kotlinUi: KotlinUi?) = kotlinUi?.let { ui -> managerTypes.map { it.name }.filter { !isOwn(it) && ui.names(managerField(it)) } }.orEmpty()

    /**
     * Whether the placeholder names a Property that exists, of any kind.
     */
    fun hasProperty(placeholder: PropertyPlaceholders.Placeholder) = propertyOf(placeholder) != null

    fun propertyOf(placeholder: PropertyPlaceholders.Placeholder) = propertiesOf(placeholder.manager)?.firstOrNull { it.name == placeholder.name }

    private fun isOwn(manager: String?) = manager == null || (kind == BlockTypeKind.MANAGER && manager == typeName)

    companion object {

        /**
         * The scope of [definition] in a project with [types] and [sceneNames].
         */
        fun of(definition: BlockTypeDefinition, types: List<BlockTypeDefinition>, sceneNames: List<String>? = null) = BlockScope(
            typeName = definition.name,
            kind = definition.kind,
            properties = definition.properties,
            actorTypeNames = types.filterIsInstance<ActorTypeDefinition>().map { it.name },
            managerTypes = types.filterIsInstance<ManagerTypeDefinition>(),
            sceneNames = sceneNames,
        )
    }
}
