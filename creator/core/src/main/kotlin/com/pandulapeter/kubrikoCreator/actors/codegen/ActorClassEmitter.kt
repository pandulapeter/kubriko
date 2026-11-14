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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.MOVEMENT_LINE
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.REMOVED_GUARD
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendAlwaysActive
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendBodyCenter
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendBounceOffFunction
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendCollisionMaskUpdate
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendDirectionFunction
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendNumberTextFunction
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendPropertyFields
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendRemovedField
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendTimerCountdown
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendTimerFields
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.appendVelocityField
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.collisionTargetOrder
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.maskClassOf
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorClassCode.maskTypeOf
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.STEP_ANIMATION_LINE
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.appendLook
import com.pandulapeter.kubrikoCreator.actors.codegen.ActorTypeCodeGenerator.appendSavedState
import com.pandulapeter.kubrikoCreator.actors.codegen.AnimateCode.appendFields
import com.pandulapeter.kubrikoCreator.actors.codegen.AnimateCode.appendFunctions
import com.pandulapeter.kubrikoCreator.actors.codegen.AnimateCode.appendSteps
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.ABSOLUTE_VALUE_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.ANIMATION_LOOPING_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.ANIMATION_SPEED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.OPACITY_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENE_OFFSET_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SET_FRAME_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STEP_ANIMATION_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.EventFunctions.appendFunction
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneBoundsCode.appendBoundsFields
import com.pandulapeter.kubrikoCreator.actors.codegen.SceneBoundsCode.appendBoundsHandling
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet

/**
 * Writes the class of an Actor type that does the work, the same way for a type of its own and for the topmost type of a
 * family, from what [features] says it needs, adding what it uses to [imports] as it writes it.
 *
 * @param scope The scope of the type, for the classes collision Events name and the code of the Events ([EventFunctions]).
 */
internal class ActorClassEmitter(
    private val features: ActorTypeFeatures,
    private val role: Role,
    private val scope: BlockScope,
    private val family: ActorTypeFamily,
    private val imports: MutableSet<String>,
) {
    /**
     * @param isFamily Whether the class is the base class of a family ([ActorTypeFeatures]' `isFamily`), whose Event
     *   functions exist for every kind of Event any of its types has, even without Blocks, since a child may override them.
     * @param visibility The visibility of the members only the class and its subclasses use.
     * @param functionModifiers The modifiers of the functions that run the Events ([EventFunctions]).
     */
    enum class Role(
        val isFamily: Boolean,
        val visibility: String,
        val functionModifiers: String,
    ) {
        /** The final class of an Actor type without a parent or children. */
        STANDALONE(isFamily = false, visibility = "private", functionModifiers = "private"),

        /** The abstract `<Name>Base` of the topmost Actor type of a family, followed by its final class. */
        FAMILY_ROOT(isFamily = true, visibility = "protected", functionModifiers = "protected open"),
    }

    private val visibility = role.visibility

    /**
     * The functions the class runs its Events in, in the order of [ActorTypeFeatures.generatedEvents].
     */
    private val functions = features.generatedEvents
        .filter { role.isFamily || it.blocks.isNotEmpty() }
        .mapNotNull { EventFunctions.of(it, scope) }
        .distinct()

    private fun callOf(event: EventDefinition) = EventFunctions.of(event, scope)?.takeIf { it in functions }?.call

    /**
     * @param properties The type's Properties, its ancestors' first.
     */
    fun StringBuilder.appendClass(
        definition: ActorTypeDefinition,
        properties: List<PropertyDefinition>,
        packageName: String,
        sheet: SpriteSheet?,
    ) {
        if (role == Role.STANDALONE) imports += features.codeImports
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
        appendBoundsFields(features.containment, features.firesEdgeEvent, features.firesLeftSceneEvent, isFamilyRoot = role == Role.FAMILY_ROOT)
        if (features.isAnimated) appendAnimationMembers()
        appendFields(features.animatedTargets, visibility)
        appendOnAdded()
        appendUpdate()
        appendKeyDispatch()
        appendCollisionDispatch()
        functions.forEach { function ->
            appendFunction(role.functionModifiers, function, definition.events.filter { EventFunctions.of(it, scope) == function }, scope, imports)
        }
        if (features.usesBounceOff) {
            imports += BOUNCE_OFF_IMPORTS
            appendBounceOffFunction(visibility)
        }
        if (features.usesDirection) {
            imports += DIRECTION_IMPORTS
            appendDirectionFunction(visibility)
        }
        if (features.animatedTargets.isNotEmpty()) {
            imports += AnimateCode.importsOf(features.animatedTargets)
            appendFunctions(features.animatedTargets, visibility)
        }
        if (role == Role.STANDALONE) appendLook(definition, imports, sheet, isFamilyMember = false, usesSetFrame = features.usesSetFrame)
        if (features.usesNumberText) appendNumberTextFunction(visibility)
        if (role == Role.STANDALONE) appendSavedState(definition, properties, packageName, imports)
        appendLine("}")
    }

    private fun StringBuilder.appendHeader(definition: ActorTypeDefinition, properties: List<PropertyDefinition>) {
        val traits = listOfNotNull(
            "Batched",
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
        appendLine()
        appendLine("    /**")
        appendLine("     * How opaque the Actor is drawn, from 0 to 1, on top of its color's own opacity; set by the opacity Blocks.")
        appendLine("     */")
        appendLine("    var $OPACITY_FIELD = 1f")
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
            val targets = features.collisionEvents.mapNotNull { it.trigger.otherActorType }.distinct().sorted()
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
        features.addedEvents.firstNotNullOfOrNull(::callOf)?.let { appendLine("        $it") }
        appendLine("    }")
    }

    private fun StringBuilder.appendUpdate() {
        if (!features.isDynamic) return
        appendLine()
        appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
        appendLine(REMOVED_GUARD)
        if (features.isAnimated) appendLine(if (role == Role.STANDALONE) "        $STEP_ANIMATION_LINE" else "        $STEP_ANIMATION_FUNCTION(deltaTimeInMilliseconds)")
        appendSteps(features.animatedTargets)
        features.timers.forEach { timer ->
            appendTimerCountdown(timer) {
                features.timerEvents.filter { it.trigger.timer == timer }.firstNotNullOfOrNull(::callOf)?.let { appendLine("                $it") }
            }
        }
        features.everyFrameEvents.firstNotNullOfOrNull(::callOf)?.let { appendLine("        $it") }
        appendLine(MOVEMENT_LINE)
        if (features.checksSceneBounds) {
            if (features.containment != SceneContainment.NONE) imports += listOf(SCENE_UNIT_IMPORT, SCENE_OFFSET_IMPORT)
            appendBoundsHandling(
                containment = features.containment,
                edgeCall = if (features.firesEdgeEvent) checkNotNull(features.edgeEvents.firstNotNullOfOrNull(::callOf)) else null,
                leftSceneCall = if (features.firesLeftSceneEvent) checkNotNull(features.leftSceneEvents.firstNotNullOfOrNull(::callOf)) else null,
            )
        }
        if (features.isCollidable) {
            if (features.shapes.size > 1) imports += features.shapes.map { MASK_IMPORT_PREFIX + maskClassOf(it) }
            appendCollisionMaskUpdate(features.shapes)
        }
        appendLine("    }")
    }

    /**
     * What plays the frames of the sprite: how fast and whether looping, which the Blocks set, and, in a family's base
     * class, the hooks a member drawn as a sprite cut into frames overrides ([ActorTypeCodeGenerator.appendLook]), since
     * the base class runs every member's Blocks and frames while only each member's final class knows its sprite.
     */
    private fun StringBuilder.appendAnimationMembers() {
        appendLine()
        appendLine("    /**")
        appendLine("     * How fast its sprite's frames play: 1 for their own pace, 0 to stop, and below 0 backwards.")
        appendLine("     */")
        appendLine("    var $ANIMATION_SPEED_FIELD = 1f")
        appendLine()
        appendLine("    /**")
        appendLine("     * Whether its sprite's frames start over after the last one.")
        appendLine("     */")
        appendLine("    var $ANIMATION_LOOPING_FIELD = true")
        if (role != Role.FAMILY_ROOT) return
        appendLine()
        appendLine("    /**")
        appendLine("     * Shows [frame] of its sprite, when it is cut into frames.")
        appendLine("     */")
        appendLine("    protected open fun $SET_FRAME_FUNCTION(frame: Int) = Unit")
        appendLine()
        appendLine("    /**")
        appendLine("     * Plays its sprite's frames for [deltaTimeInMilliseconds], when it is cut into frames.")
        appendLine("     */")
        appendLine("    protected open fun $STEP_ANIMATION_FUNCTION(deltaTimeInMilliseconds: Int) = Unit")
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
        features.keyEvents.groupBy { it.trigger.key!! }.toSortedMap().forEach { (key, events) ->
            events.firstNotNullOfOrNull(::callOf)?.let { appendLine("            Key.${key.composeKeyName} -> $it") }
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
        features.collisionEvents.groupBy { it.trigger.otherActorType!! }.toSortedMap(collisionTargetOrder(family)).forEach { (other, events) ->
            events.firstNotNullOfOrNull(::callOf)?.let { appendLine("                is ${scope.classOf(other)} -> if (!other.$REMOVED_FIELD) $it") }
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
            "Batched" to "com.pandulapeter.kubriko.actor.traits.Batched",
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
        val DIRECTION_IMPORTS = listOf(
            SCENE_OFFSET_IMPORT,
            "com.pandulapeter.kubriko.helpers.extensions.sceneUnit",
            "kotlin.math.cos",
            "kotlin.math.sin",
        )
    }
}
