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

import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.COLLISION_MASK
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.MOVEMENT_LINE
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.REMOVED_GUARD
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendAlwaysActive
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendBounceOffFunction
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendCollisionMaskUpdate
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendEdgeHandling
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.appendRemovedField
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.collisionTargetOrder
import com.pandulapeter.kubrikoCreator.actors.ActorClassCode.maskTypeOf
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.GENERATED_PACKAGE
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.SPRITE_DRAW_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.appendSavedState
import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator.drawFunction
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.INDENT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.eventLines
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.templateLiteral

/**
 * Turns an Actor type that has a parent or children into Kotlin that inherits as the Actor types do. The topmost Actor
 * type of a family gets an abstract base class (`EnemyBase`) with everything the whole family does, each Event an `open`
 * hook; a type with children gets a base class of its own that overrides the hooks of its Events; and every type gets a
 * final class that keeps its state, draws it, and builds its collision mask. "Run the parent's Event" is the `super`
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
        val features = ActorTypeFeatures(
            events = members.flatMap { family.eventsOf(it.name) },
            scope = scopeOf(byName.getValue(root)),
            packageName = packageName,
            shapes = members.mapTo(mutableSetOf()) { it.shape },
            hasSprite = members.any { it.sprite?.let(SpriteFiles::accessorOf) != null },
            isCollidableAnyway = members.any { it.name in collidableActorTypeNames },
        )
        val hooks = Hooks(features, scopeOf(byName.getValue(root)))
        val usesNumberText = members.any { member ->
            member.events.any { event -> event.blocks.any { it is BlockDefinition.Log && ".$NUMBER_TEXT_FUNCTION()" in templateLiteral(it.message, scopeOf(member)) } }
        }
        val imports = (features.imports + listOfNotNull(
            "com.pandulapeter.kubriko.collision.mask.$COLLISION_MASK".takeIf { features.isCollidable && maskTypeOf(features.shapes) == COLLISION_MASK },
        )).sorted()
        return GeneratedFile.withHeader(" from .kubriko/actors/$name.json", buildString {
            appendLine("package $packageName.$GENERATED_PACKAGE")
            appendLine()
            imports.forEach { appendLine("import $it") }
            appendLine()
            if (name == root) {
                appendRootBase(definition, family, features, hooks, usesNumberText) { scopeOf(definition, it) }
                appendLine()
            } else if (family.hasChildren(name)) {
                appendBase(definition, family, features, hooks) { scopeOf(definition, it) }
                appendLine()
            }
            appendConcrete(definition, family, features, hooks) { scopeOf(definition, it) }
        })
    }

    /**
     * The abstract class of the topmost Actor type of a family: everything the family does, with its own Events as the
     * hooks' default code.
     */
    private fun StringBuilder.appendRootBase(
        definition: ActorTypeDefinition,
        family: ActorTypeFamily,
        features: ActorTypeFeatures,
        hooks: Hooks,
        usesNumberText: Boolean,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        val baseName = baseNameOf(definition.name)
        val properties = family.propertiesOf(definition.name)
        val traits = listOfNotNull(
            "Visible",
            "Dynamic".takeIf { features.isDynamic },
            if (features.collisionEvents.isNotEmpty()) "CollisionDetector" else "Collidable".takeIf { features.isCollidable },
            "KeyboardInputAware".takeIf { features.keyEvents.isNotEmpty() },
        )
        appendLine("/**")
        appendLine(" * What every ${definition.name} does, and every Actor type that inherits from it.")
        appendLine(" */")
        appendLine("abstract class $baseName(")
        appendLine("    final override val body: BoxBody,")
        appendLine("    color: Color,")
        properties.forEach { appendLine("    ${it.name}: ${PropertyKind.of(it).kotlinType},") }
        if (features.isCollidable) appendLine("    final override val collisionMask: ${maskTypeOf(features.shapes)},")
        appendLine(") : ${traits.joinToString()} {")
        appendLine("    @set:Exposed(name = \"color\")")
        appendLine("    var color: Color = color")
        if (features.isDynamic) {
            appendLine()
            appendLine("    /**")
            appendLine("     * In scene units per second: the Actor moves by this much every second.")
            appendLine("     */")
            appendLine("    var velocity = SceneOffset.Zero")
            appendAlwaysActive()
        }
        appendRemovedField()
        appendPropertyFields(properties)
        if (features.isCollidable && features.isDynamic) {
            appendLine()
            appendLine("    /**")
            appendLine("     * The middle of the Actor in the Scene, wherever its pivot is, which is where its collision mask goes.")
            appendLine("     */")
            appendLine("    private val bodyCenter get() = body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }")
        }
        if (features.collisionEvents.isNotEmpty()) {
            val targets = features.collisionEvents.mapNotNull { it.otherActorType }.distinct().sorted()
            appendLine("    override val collidableTypes: List<KClass<out Collidable>> = listOf(${targets.joinToString { "${hooks.scope.classOf(it)}::class" }})")
        }
        features.lookups.forEach { appendLine("    protected lateinit var $it") }
        features.timers.forEach { timer ->
            appendLine("    protected var ${TimerNaming.remainingField(timer)} = 0L")
            appendLine("    protected var ${TimerNaming.periodField(timer)} = 0L")
        }
        if (features.lookups.isNotEmpty() || features.addedEvents.isNotEmpty()) {
            appendLine()
            appendLine("    override fun onAdded(kubriko: Kubriko) {")
            features.lookups.forEach { appendLine("        ${it.substringBefore(':')} = kubriko.get()") }
            if (features.addedEvents.isNotEmpty()) appendLine("        ${Hooks.ADDED}()")
            appendLine("    }")
        }
        if (features.isDynamic) {
            appendLine()
            appendLine("    override fun update(deltaTimeInMilliseconds: Int) {")
            appendLine(REMOVED_GUARD)
            features.timers.forEach { timer ->
                val remaining = TimerNaming.remainingField(timer)
                val period = TimerNaming.periodField(timer)
                appendLine("        if ($remaining > 0) {")
                appendLine("            $remaining -= deltaTimeInMilliseconds")
                appendLine("            if ($remaining <= 0) {")
                appendLine("                $remaining = if ($period > 0) ($remaining + $period).coerceAtLeast(1) else 0")
                if (features.timerEvents.any { it.timer == timer }) appendLine("                ${Hooks.timer(timer)}()")
                appendLine("            }")
                appendLine("        }")
            }
            if (features.everyFrameEvents.isNotEmpty()) appendLine("        ${Hooks.EVERY_FRAME}()")
            appendLine(MOVEMENT_LINE)
            if (features.edgeEvents.isNotEmpty()) appendEdgeHandling { appendLine("            ${Hooks.EDGE}(${Hooks.EDGE_ARGUMENTS})") }
            if (features.isCollidable) appendCollisionMaskUpdate(features.shapes)
            appendLine("    }")
        }
        if (features.keyEvents.isNotEmpty()) {
            appendLine()
            appendLine("    override fun onKeyPressed(key: Key) {")
            // Kubriko keeps telling Actors about key presses while the game is paused.
            appendLine("        if (!$STATE_MANAGER_FIELD.isRunning.value) return")
            appendLine("        when (key) {")
            features.keyEvents.mapNotNull { it.key }.distinct().sortedBy { it.composeKeyName }.forEach { key ->
                appendLine("            Key.${key.composeKeyName} -> ${Hooks.key(key)}()")
            }
            appendLine("        }")
            appendLine("    }")
        }
        if (features.collisionEvents.isNotEmpty()) {
            appendLine()
            appendLine("    override fun onCollisionDetected(collidables: List<Collidable>) {")
            appendLine(REMOVED_GUARD)
            appendLine("        for (index in collidables.indices) {")
            appendLine("            when (val other = collidables[index]) {")
            features.collisionEvents.mapNotNull { it.otherActorType }.distinct().sortedWith(collisionTargetOrder(family)).forEach { other ->
                appendLine("                is ${hooks.scope.classOf(other)} -> if (!other.$REMOVED_FIELD) ${Hooks.collision(other)}(other)")
            }
            appendLine("            }")
            appendLine("        }")
            appendLine("    }")
        }
        hooks.all.forEach { hook ->
            val own = definition.events.filter { hooks.of(it) == hook }
            appendLine()
            appendLine("    protected open fun ${hook.signature} {")
            own.forEach { event -> eventLines(event.blocks, event.type, scopeOf(null)).forEach { appendLine(INDENT.repeat(2) + it) } }
            appendLine("    }")
        }
        if (features.usesBounceOff) appendBounceOffFunction("protected")
        if (usesNumberText) {
            appendLine()
            appendLine("    protected fun Float.$NUMBER_TEXT_FUNCTION() = if (this % 1f == 0f) toLong().toString() else toString()")
        }
        appendLine("}")
    }

    /**
     * The abstract class of an Actor type with a parent and children: its new Properties, and its Events as overrides.
     */
    private fun StringBuilder.appendBase(
        definition: ActorTypeDefinition,
        family: ActorTypeFamily,
        features: ActorTypeFeatures,
        hooks: Hooks,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        val inherited = family.propertiesOf(checkNotNull(family.parentOf(definition.name)))
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
        appendPropertyFields(newPropertiesOf(definition.name, family))
        appendOverrides(definition, hooks, scopeOf)
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
        hooks: Hooks,
        scopeOf: (parentEventCall: String?) -> BlockScope,
    ) {
        val name = definition.name
        val hasChildren = family.hasChildren(name)
        val superName = baseNameOf(if (hasChildren) name else checkNotNull(family.parentOf(name)))
        val superProperties = family.propertiesOf(if (hasChildren) name else family.parentOf(name)!!)
        appendLine("class $name private constructor(state: State) : $superName(")
        appendLine("    body = state.body,")
        appendLine("    color = state.color,")
        superProperties.forEach { appendLine("    ${it.name} = state.${it.name},") }
        if (features.isCollidable) appendLine("    collisionMask = ${collisionMaskOf(definition.shape)},")
        appendLine("), Editable<$name> {")
        if (!hasChildren) {
            appendPropertyFields(newPropertiesOf(name, family), source = "state.")
            appendOverrides(definition, hooks, scopeOf)
        }
        appendLine()
        val spriteAccessor = definition.sprite?.let(SpriteFiles::accessorOf)
        appendLine(if (spriteAccessor == null) drawFunction(definition.shape) else SPRITE_DRAW_FUNCTION)
        appendSavedState(definition, family.propertiesOf(name), spriteAccessor)
        appendLine("}")
    }

    private fun StringBuilder.appendOverrides(definition: ActorTypeDefinition, hooks: Hooks, scopeOf: (parentEventCall: String?) -> BlockScope) {
        definition.events.groupBy { hooks.of(it) }.forEach { (hook, events) ->
            if (hook == null) return@forEach
            appendLine()
            appendLine("    override fun ${hook.signature} {")
            events.forEach { event -> eventLines(event.blocks, event.type, scopeOf("super.${hook.call}")).forEach { appendLine(INDENT.repeat(2) + it) } }
            appendLine("    }")
        }
    }

    /**
     * @param source Where the initial values come from: the constructor's parameters of the same names, or the state's.
     */
    private fun StringBuilder.appendPropertyFields(properties: List<PropertyDefinition>, source: String = "") = properties.forEach { property ->
        appendLine()
        appendLine("    @set:Exposed(name = ${stringLiteral(property.name)})")
        appendLine("    var ${property.name}: ${PropertyKind.of(property).kotlinType} = $source${property.name}")
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

    /**
     * The hooks a family's base class has: one per kind of Event any of its Actor types reacts to.
     *
     * @param scope The scope of the family's topmost Actor type, for the classes collision Events name.
     */
    private class Hooks(features: ActorTypeFeatures, val scope: BlockScope) {

        data class Hook(val signature: String, val call: String)

        val all = features.generatedEvents.mapNotNull(::of).distinct()

        fun of(event: EventDefinition): Hook? = when (event.type) {
            EventType.ADDED_TO_SCENE -> Hook("$ADDED()", "$ADDED()")
            EventType.EVERY_FRAME -> Hook("$EVERY_FRAME()", "$EVERY_FRAME()")
            EventType.EDGE_TOUCHED -> Hook("$EDGE($EDGE_PARAMETERS)", "$EDGE($EDGE_ARGUMENTS)")
            EventType.KEY_PRESSED -> event.key?.takeIf { it.isKnown }?.let { Hook("${key(it)}()", "${key(it)}()") }
            EventType.COLLISION -> event.otherActorType?.takeIf { it in scope.actorTypeNames }?.let { other ->
                Hook("${collision(other)}(other: ${scope.classOf(other)})", "${collision(other)}(other)")
            }
            EventType.TIMER_FINISHED -> event.timer?.takeIf(TimerNaming::isValid)?.let { Hook("${timer(it)}()", "${timer(it)}()") }
            EventType.GAME_STARTED -> null
        }

        companion object {
            const val ADDED = "onAddedToScene"
            const val EVERY_FRAME = "onEveryFrame"
            const val EDGE = "onEdgeTouched"
            const val EDGE_ARGUMENTS = "isTouchingLeftEdge, isTouchingRightEdge, isTouchingTopEdge, isTouchingBottomEdge"
            const val EDGE_PARAMETERS = "isTouchingLeftEdge: Boolean, isTouchingRightEdge: Boolean, isTouchingTopEdge: Boolean, isTouchingBottomEdge: Boolean"

            fun key(key: KeyChoice) = "onKeyPressed${key.composeKeyName}"

            fun collision(other: String) = "onCollisionWith$other"

            fun timer(timer: String) = TimerNaming.hookName(timer)
        }
    }
}
