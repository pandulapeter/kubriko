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

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendPropertyFields
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.maskClassOf
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.maskTypeOf
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.GENERATED_PACKAGE
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.appendLook
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.appendSavedState
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.eventCode
import com.pandulapeter.kubrikoCreator.actors.codegen.EventFunctions.appendFunction

/**
 * Turns an Actor type that has a parent or children into Kotlin that inherits as the Actor types do. The topmost Actor
 * type of a family gets an abstract base class (`EnemyBase`) with everything the whole family does, each Event an `open`
 * hook; a type with children gets a base class of its own that overrides the hooks of its Events; and every type gets a
 * final class that keeps its state, holds its look, and builds its collision mask. "Run the parent's Event" is the `super`
 * call of the hook.
 */
internal object ActorFamilyCodeGenerator {

    /**
     * @param actorTypes Every Actor type of the project, which make up the families.
     */
    fun generate(
        definition: ActorTypeDefinition,
        packageName: String,
        actorTypes: List<ActorTypeDefinition>,
        managerTypes: List<ManagerTypeDefinition>,
        collidableActorTypeNames: Set<String>,
        family: ActorTypeFamily = ActorTypeFamily(actorTypes),
    ): String {
        val byName = actorTypes.associateBy { it.name }
        val name = definition.name
        val root = family.rootOf(name)
        val members = family.familyOf(root).mapNotNull(byName::get)
        val types = actorTypes + managerTypes
        fun scopeOf(type: ActorTypeDefinition, parentEventCall: String? = null) = BlockScope.of(type, types, family = family).copy(parentEventCall = parentEventCall)
        val rootScope = scopeOf(byName.getValue(root))
        val role = ActorClassEmitter.Role.FAMILY_ROOT
        val features = ActorTypeFeatures(
            events = members.flatMap { family.eventsOf(it.name) },
            scope = rootScope,
            shapes = members.mapTo(mutableSetOf()) { it.shape },
            hasSprite = members.any { it.sprite?.let(SpriteFiles::accessorOf) != null },
            isCollidableAnyway = members.any { it.name in collidableActorTypeNames },
            isFamily = role.isFamily,
            eventScopes = members.map { it.events to scopeOf(it) },
        )
        val hookOf = { event: EventDefinition -> EventFunctions.of(event, rootScope) }
        val imports = definition.events.filter { hookOf(it) != null }
            .fold(BlockCode.NONE) { code, event -> code + eventCode(event.blocks, event.type, scopeOf(definition)) }
            .imports.toMutableSet()
        val code = buildString {
            if (name == root) {
                with(ActorClassEmitter(features, role, rootScope, family, imports)) { appendClass(definition, family.propertiesOf(name), packageName) }
                appendLine()
            } else if (family.hasChildren(name)) {
                appendBase(definition, family, features, hookOf, imports) { scopeOf(definition, it) }
                appendLine()
            }
            appendConcrete(definition, family, features, hookOf, packageName, imports) { scopeOf(definition, it) }
        }
        return GeneratedFile.withHeader(" from ${DefinitionPaths.definitionPath(definition)}", buildString {
            appendLine("package $packageName.$GENERATED_PACKAGE")
            appendLine()
            imports.sorted().forEach { appendLine("import $it") }
            appendLine()
            append(code)
        })
    }

    /**
     * The abstract class of an Actor type with a parent and children: its new Properties, and its Events as overrides.
     */
    private fun StringBuilder.appendBase(
        definition: ActorTypeDefinition,
        family: ActorTypeFamily,
        features: ActorTypeFeatures,
        hookOf: (EventDefinition) -> EventFunctions.Function?,
        imports: MutableSet<String>,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        val inherited = family.propertiesOf(checkNotNull(family.parentOf(definition.name)))
        val newProperties = newPropertiesOf(definition.name, family)
        imports += listOf("com.pandulapeter.kubriko.actor.body.BoxBody", "androidx.compose.ui.graphics.Color")
        if (features.isCollidable) imports += MASK_IMPORT_PREFIX + maskTypeOf(features.shapes)
        if (newProperties.isNotEmpty()) imports += EXPOSED_IMPORT
        appendLine("/**")
        appendLine(" * What every ${definition.name} does, and every Actor type that inherits from it.")
        appendLine(" */")
        appendLine("abstract class ${baseNameOf(definition.name)}(")
        appendLine("    body: BoxBody,")
        appendLine("    color: Color,")
        family.propertiesOf(definition.name).forEach { appendLine("    ${it.name}: ${PropertyKind.of(it).kotlinType},") }
        if (features.isCollidable) appendLine("    collisionMask: ${maskTypeOf(features.shapes)},")
        appendLine(") : ${baseNameOf(family.parentOf(definition.name)!!)}(")
        appendLine("    body = body,")
        appendLine("    color = color,")
        inherited.forEach { appendLine("    ${it.name} = ${it.name},") }
        if (features.isCollidable) appendLine("    collisionMask = collisionMask,")
        appendLine(") {")
        appendPropertyFields(newProperties)
        appendOverrides(definition, hookOf, imports, scopeOf)
        appendLine("}")
    }

    /**
     * The final class of an Actor type in a family: its state, drawing, and collision mask, and, for one without children,
     * its new Properties and its Events as overrides.
     */
    private fun StringBuilder.appendConcrete(
        definition: ActorTypeDefinition,
        family: ActorTypeFamily,
        features: ActorTypeFeatures,
        hookOf: (EventDefinition) -> EventFunctions.Function?,
        packageName: String,
        imports: MutableSet<String>,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        val name = definition.name
        val hasChildren = family.hasChildren(name)
        val newProperties = if (hasChildren) emptyList() else newPropertiesOf(name, family)
        imports += "com.pandulapeter.kubriko.sceneEditor.Editable"
        if (features.isCollidable) imports += MASK_IMPORT_PREFIX + maskClassOf(definition.shape)
        if (newProperties.isNotEmpty()) imports += EXPOSED_IMPORT
        val superName = baseNameOf(if (hasChildren) name else checkNotNull(family.parentOf(name)))
        val superProperties = family.propertiesOf(if (hasChildren) name else family.parentOf(name)!!)
        appendLine("class $name private constructor(state: State) : $superName(")
        appendLine("    body = state.body,")
        appendLine("    color = state.color,")
        superProperties.forEach { appendLine("    ${it.name} = state.${it.name},") }
        if (features.isCollidable) appendLine("    collisionMask = ${collisionMaskOf(definition.shape)},")
        appendLine("), Editable<$name> {")
        if (!hasChildren) {
            appendPropertyFields(newProperties, source = "state.")
            appendOverrides(definition, hookOf, imports, scopeOf)
        }
        appendLook(definition, imports)
        appendSavedState(definition, family.propertiesOf(name), packageName, imports)
        appendLine("}")
    }

    private fun StringBuilder.appendOverrides(
        definition: ActorTypeDefinition,
        hookOf: (EventDefinition) -> EventFunctions.Function?,
        imports: MutableSet<String>,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        definition.events.groupBy(hookOf).forEach { (hook, events) ->
            if (hook != null) appendFunction("override", hook, events, scopeOf("super.${hook.call}"), imports)
        }
    }

    /**
     * The Properties [name] adds to its parent's.
     */
    private fun newPropertiesOf(name: String, family: ActorTypeFamily): List<PropertyDefinition> {
        val inherited = family.parentOf(name)?.let(family::propertiesOf).orEmpty().map { it.name }.toSet()
        return family.propertiesOf(name).filter { it.name !in inherited }
    }

    private fun collisionMaskOf(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> "CircleCollisionMask(initialPosition = state.body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }, " +
            "initialRadius = state.body.size.width * state.body.scale.horizontal / 2)"
        ActorShape.RECTANGLE -> "BoxCollisionMask(initialPosition = state.body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }, " +
            "initialSize = state.body.size * state.body.scale, initialRotation = state.body.rotation)"
    }

    fun baseNameOf(name: String) = name + BlockTypeNaming.BASE_SUFFIX

    private const val MASK_IMPORT_PREFIX = "com.pandulapeter.kubriko.collision.mask."
    private const val EXPOSED_IMPORT = "com.pandulapeter.kubriko.sceneEditor.Exposed"
}
