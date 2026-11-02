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

import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.MOVEMENT_LINE
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.REMOVED_GUARD
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendAlwaysActive
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendBodyCenter
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendBounceOffFunction
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendCollisionMaskUpdate
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendEdgeHandling
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendNumberTextFunction
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendPropertyFields
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendRemovedField
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendTimerCountdown
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendTimerFields
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendVelocityField
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.collisionTargetOrder
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.maskClassOf
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.maskTypeOf
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.appendDrawFunction
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.appendSavedState
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.ABSOLUTE_VALUE_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENE_OFFSET_IMPORT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendEvents
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.appendTimer

/**
 * Writes the class of an Actor type that does the work, the same way for a type of its own and for the topmost type of a
 * family, from what [features] says it needs, adding what it uses to [imports] as it writes it.
 *
 * @param scope The scope of the type, for the classes collision Events name and the code of the Events written in place.
 */
internal class ActorClassEmitter(
    private val features: ActorTypeFeatures,
    private val role: Role,
    private val scope: BlockScope,
    private val family: ActorTypeFamily,
    private val imports: MutableSet<String>,
) {
    /**
     * @param isFamily Whether the class is the base class of a family ([ActorTypeFeatures]' `isFamily`).
     * @param visibility The visibility of the members only the class and its subclasses use.
     */
    enum class Role(
        val isFamily: Boolean,
        val visibility: String,
        val eventPlacement: EventPlacement,
    ) {
        /** The final class of an Actor type without a parent or children. */
        STANDALONE(isFamily = false, visibility = "private", eventPlacement = EventPlacement.INLINE),

        /** The abstract `<Name>Base` of the topmost Actor type of a family, followed by its final class. */
        FAMILY_ROOT(isFamily = true, visibility = "protected", eventPlacement = EventPlacement.HOOK_CALLS),
    }

    /**
     * How the code of the Events gets into `onAdded`, `update`, `onKeyPressed`, and `onCollisionDetected`: written in
     * place, or as calls of the `open` hooks a family's Actor types override ([FamilyHooks]).
     */
    enum class EventPlacement { INLINE, HOOK_CALLS }

    private val visibility = role.visibility
    private val placement = role.eventPlacement

    /**
     * @param properties The type's Properties, its ancestors' first.
     * @param appendEventFunctions Writes the functions that run the Events, if they aren't written in place.
     */
    fun StringBuilder.appendClass(
        definition: ActorTypeDefinition,
        properties: List<PropertyDefinition>,
        packageName: String,
        appendEventFunctions: StringBuilder.() -> Unit = {},
    ) {
        if (placement == EventPlacement.INLINE) imports += features.codeImports
        appendHeader(definition, properties)
        if (features.isDynamic) {
            imports += SCENE_OFFSET_IMPORT
            appendVelocityField()
            appendAlwaysActive()
        }
        appendRemovedField()
        appendProperties(properties)
        appendCollisionMembers(definition.shape)
        features.lookups.forEach { appendLine("    $visibility lateinit var $it") }
        imports += features.lookupImports
        appendTimerFields(features.timers, visibility)
        appendOnAdded()
        appendUpdate()
        appendKeyDispatch()
        appendCollisionDispatch()
        appendEventFunctions()
        if (features.usesBounceOff) {
            imports += BOUNCE_OFF_IMPORTS
            appendBounceOffFunction(visibility)
        }
        if (role == Role.STANDALONE) appendDrawFunction(definition, imports)
        if (features.usesNumberText) appendNumberTextFunction(visibility)
        if (role == Role.STANDALONE) appendSavedState(definition, properties, packageName, imports)
        appendLine("}")
    }

    private fun StringBuilder.appendHeader(definition: ActorTypeDefinition, properties: List<PropertyDefinition>) {
        val traits = listOfNotNull(
            "Visible",
            "Dynamic".takeIf { features.isDynamic },
            if (features.collisionEvents.isNotEmpty()) "CollisionDetector" else "Collidable".takeIf { features.isCollidable },
            "Editable<${definition.name}>".takeIf { role == Role.STANDALONE },
            "KeyboardInputAware".takeIf { features.keyEvents.isNotEmpty() },
        )
        traits.forEach { trait -> imports += TRAIT_IMPORTS.getValue(trait.substringBefore('<')) }
        imports += "androidx.compose.ui.graphics.Color"
        imports += "com.pandulapeter.kubriko.sceneEditor.Exposed"
        when (role) {
            Role.STANDALONE -> {
                appendLine("class ${definition.name} private constructor(state: State) : ${traits.joinToString()} {")
                appendLine("    override val body = state.body")
                appendLine()
            }
            Role.FAMILY_ROOT -> {
                imports += "com.pandulapeter.kubriko.actor.body.BoxBody"
                appendLine("/**")
                appendLine(" * What every ${definition.name} does, and every Actor type that inherits from it.")
                appendLine(" */")
                appendLine("abstract class ${ActorFamilyCodeGenerator.baseNameOf(definition.name)}(")
                appendLine("    final override val body: BoxBody,")
                appendLine("    color: Color,")
                properties.forEach { appendLine("    ${it.name}: ${PropertyKind.of(it).kotlinType},") }
                if (features.isCollidable) {
                    imports += MASK_IMPORT_PREFIX + maskTypeOf(features.shapes)
                    appendLine("    final override val collisionMask: ${maskTypeOf(features.shapes)},")
                }
                appendLine(") : ${traits.joinToString()} {")
            }
        }
        appendLine("    @set:Exposed(name = \"color\")")
        appendLine("    var color: Color = ${if (role == Role.STANDALONE) "state.color" else "color"}")
    }

    private fun StringBuilder.appendProperties(properties: List<PropertyDefinition>) = when (role) {
        Role.STANDALONE -> appendPropertyFields(properties, source = "state.")
        Role.FAMILY_ROOT -> appendPropertyFields(properties)
    }

    /**
     * The members of a collidable Actor: a type of its own builds its mask around the middle of its body, which a family's
     * base class takes in its constructor and moves there only if it moves.
     */
    private fun StringBuilder.appendCollisionMembers(shape: ActorShape) {
        when (role) {
            Role.STANDALONE -> if (features.isCollidable) {
                imports += MASK_IMPORT_PREFIX + maskClassOf(shape)
                appendBodyCenter()
                appendLine(collisionMask(shape))
            }
            Role.FAMILY_ROOT -> if (features.isCollidable && features.isDynamic) appendBodyCenter()
        }
        if (features.collisionEvents.isNotEmpty()) {
            imports += listOf("kotlin.reflect.KClass", COLLIDABLE_IMPORT)
            val targets = features.collisionEvents.mapNotNull { it.otherActorType }.distinct().sorted()
            appendLine("    override val collidableTypes: List<KClass<out Collidable>> = listOf(${targets.joinToString { "${scope.classOf(it)}::class" }})")
        }
    }

    private fun StringBuilder.appendOnAdded() {
        if (!features.overridesOnAdded) return
        imports += "com.pandulapeter.kubriko.Kubriko"
        if (features.lookups.isNotEmpty()) imports += "com.pandulapeter.kubriko.helpers.extensions.get"
        appendLine()
        appendLine("    override fun onAdded(kubriko: Kubriko) {")
        features.lookups.forEach { appendLine("        ${it.substringBefore(':')} = kubriko.get()") }
        when (placement) {
            EventPlacement.INLINE -> appendEvents(features.addedEvents, depth = 2, scope = scope)
            EventPlacement.HOOK_CALLS -> if (features.addedEvents.isNotEmpty()) appendLine("        ${FamilyHooks.ADDED}()")
        }
        appendLine("    }")
    }

    private fun StringBuilder.appendUpdate() {
        if (!features.isDynamic) return
        appendLine()
        appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
        appendLine(REMOVED_GUARD)
        features.timers.forEach { timer ->
            val events = features.timerEvents.filter { it.timer == timer }
            when (placement) {
                EventPlacement.INLINE -> appendTimer(timer, events, scope)
                EventPlacement.HOOK_CALLS -> appendTimerCountdown(timer, isCommented = false) {
                    if (events.isNotEmpty()) appendLine("                ${FamilyHooks.timer(timer)}()")
                }
            }
        }
        when (placement) {
            EventPlacement.INLINE -> appendEvents(features.everyFrameEvents, depth = 2, scope = scope)
            EventPlacement.HOOK_CALLS -> if (features.everyFrameEvents.isNotEmpty()) appendLine("        ${FamilyHooks.EVERY_FRAME}()")
        }
        appendLine(MOVEMENT_LINE)
        if (features.edgeEvents.isNotEmpty()) {
            imports += listOf(SCENE_UNIT_IMPORT, SCENE_OFFSET_IMPORT)
            appendEdgeHandling {
                when (placement) {
                    EventPlacement.INLINE -> appendEvents(features.edgeEvents, depth = 3, scope = scope)
                    EventPlacement.HOOK_CALLS -> appendLine("            ${FamilyHooks.EDGE}(${FamilyHooks.EDGE_ARGUMENTS})")
                }
            }
        }
        if (features.isCollidable) {
            if (features.shapes.size > 1) imports += features.shapes.map { MASK_IMPORT_PREFIX + maskClassOf(it) }
            appendCollisionMaskUpdate(features.shapes)
        }
        appendLine("    }")
    }

    /**
     * Kubriko keeps telling Actors about key presses while the game is paused, so they return early then.
     */
    private fun StringBuilder.appendKeyDispatch() {
        if (features.keyEvents.isEmpty()) return
        imports += listOf(KEY_IMPORT, TRAIT_IMPORTS.getValue("KeyboardInputAware"))
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        appendLine("        if (!$STATE_MANAGER_FIELD.isRunning.value) return")
        appendLine("        when (key) {")
        features.keyEvents.groupBy { it.key!! }.toSortedMap().forEach { (key, events) ->
            when (placement) {
                EventPlacement.INLINE -> {
                    appendLine("            Key.${key.composeKeyName} -> {")
                    appendEvents(events, depth = 4, scope = scope)
                    appendLine("            }")
                }
                EventPlacement.HOOK_CALLS -> appendLine("            Key.${key.composeKeyName} -> ${FamilyHooks.key(key)}()")
            }
        }
        appendLine("        }")
        appendLine("    }")
    }

    private fun StringBuilder.appendCollisionDispatch() {
        if (features.collisionEvents.isEmpty()) return
        imports += COLLIDABLE_IMPORT
        appendLine()
        appendLine("    override fun onCollisionDetected(collidables: List<Collidable>) {")
        appendLine(REMOVED_GUARD)
        appendLine("        for (index in collidables.indices) {")
        appendLine("            when (val other = collidables[index]) {")
        features.collisionEvents.groupBy { it.otherActorType!! }.toSortedMap(collisionTargetOrder(family)).forEach { (other, events) ->
            when (placement) {
                EventPlacement.INLINE -> {
                    appendLine("                is ${scope.classOf(other)} -> if (!other.$REMOVED_FIELD) {")
                    appendEvents(events, depth = 5, scope = scope)
                    appendLine("                }")
                }
                EventPlacement.HOOK_CALLS -> appendLine("                is ${scope.classOf(other)} -> if (!other.$REMOVED_FIELD) ${FamilyHooks.collision(other)}(other)")
            }
        }
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
    }

    private fun collisionMask(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> """
            |    override val collisionMask = CircleCollisionMask(
            |        initialPosition = bodyCenter,
            |        initialRadius = body.size.width * body.scale.horizontal / 2,
            |    )
        """.trimMargin()
        ActorShape.RECTANGLE -> """
            |    override val collisionMask = BoxCollisionMask(
            |        initialPosition = bodyCenter,
            |        initialSize = body.size * body.scale,
            |        initialRotation = body.rotation,
            |    )
        """.trimMargin()
    }

    private companion object {
        const val MASK_IMPORT_PREFIX = "com.pandulapeter.kubriko.collision.mask."
        const val COLLIDABLE_IMPORT = "com.pandulapeter.kubriko.collision.Collidable"
        const val SCENE_UNIT_IMPORT = "com.pandulapeter.kubriko.types.SceneUnit"
        const val KEY_IMPORT = "androidx.compose.ui.input.key.Key"
        val TRAIT_IMPORTS = mapOf(
            "Visible" to "com.pandulapeter.kubriko.actor.traits.Visible",
            "Dynamic" to "com.pandulapeter.kubriko.actor.traits.Dynamic",
            "CollisionDetector" to "com.pandulapeter.kubriko.collision.CollisionDetector",
            "Collidable" to COLLIDABLE_IMPORT,
            "Editable" to "com.pandulapeter.kubriko.sceneEditor.Editable",
            "KeyboardInputAware" to "com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware",
        )
        val BOUNCE_OFF_IMPORTS = listOf(
            ABSOLUTE_VALUE_IMPORT,
            SCENE_OFFSET_IMPORT,
            SCENE_UNIT_IMPORT,
            "com.pandulapeter.kubriko.actor.traits.Visible",
            "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
        )
    }
}
