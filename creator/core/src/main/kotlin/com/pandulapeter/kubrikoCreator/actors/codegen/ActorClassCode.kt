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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.BOUNCE_OFF_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.DIRECTION_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.NUMBER_TEXT_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STEERING_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral

/**
 * The parts of a generated Actor class that a type of its own ([ActorTypeCodeGenerator]) and the base class of a family
 * ([ActorFamilyCodeGenerator]) share, so that the two behave the same, some of which Manager types
 * ([ManagerTypeCodeGenerator]) share too.
 */
internal object ActorClassCode {

    /**
     * The longest step a frame moves an Actor by, so that after a hitch a fast Actor doesn't pass through a thin one.
     */
    private const val MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS = 33

    const val MOVEMENT_LINE = "        body.position += velocity * (deltaTimeInMilliseconds.coerceAtMost($MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS) / 1000f)"

    /**
     * Skips the rest of a function for an Actor that a Block removed, since removing only happens after the frame.
     */
    const val REMOVED_GUARD = "        if ($REMOVED_FIELD) return"

    /**
     * Whether the Actor was removed by a Block, set right away, while removing happens after the frame: until then it
     * neither updates nor collides, so it can't be hit, or score, twice.
     */
    fun StringBuilder.appendRemovedField() {
        appendLine()
        appendLine("    /**")
        appendLine("     * Set by the Blocks that remove this Actor, which Kubriko does after the frame: until then, it is skipped.")
        appendLine("     */")
        appendLine("    var $REMOVED_FIELD = false")
    }

    fun StringBuilder.appendVelocityField() {
        appendLine()
        appendLine("    /**")
        appendLine("     * In scene units per second: the Actor moves by this much every second.")
        appendLine("     */")
        appendLine("    var velocity = SceneOffset.Zero")
    }

    fun StringBuilder.appendBodyCenter() {
        appendLine()
        appendLine("    /**")
        appendLine("     * The middle of the Actor in the Scene, wherever its pivot is, which is where its collision mask goes.")
        appendLine("     */")
        appendLine("    private val bodyCenter get() = body.axisAlignedBoundingBox.let { (it.min + it.max) / 2 }")
    }

    /**
     * @param visibility `private` for a type of its own or a Manager type, `protected` for a family's base class.
     */
    fun StringBuilder.appendTimerFields(timers: List<String>, visibility: String) = timers.forEach { timer ->
        appendLine()
        appendLine("    /**")
        appendLine("     * Until $timer finishes, or 0 while it doesn't run, and how often it finishes again, or 0 if it doesn't.")
        appendLine("     */")
        appendLine("    $visibility var ${TimerNaming.remainingField(timer)} = 0L")
        appendLine("    $visibility var ${TimerNaming.periodField(timer)} = 0L")
    }

    /**
     * @param source Where the initial values come from: the constructor's parameters of the same names, or the state's.
     */
    fun StringBuilder.appendPropertyFields(properties: List<PropertyDefinition>, source: String = "") = properties.forEach { property ->
        appendLine()
        appendLine("    @set:Exposed(name = ${stringLiteral(property.name)})")
        appendLine("    var ${property.name}: ${PropertyKind.of(property).kotlinType} = $source${property.name}")
    }

    /**
     * Counts [timer] down in `update` and, when it finishes, starts it again if it repeats and runs [onFinished]. A
     * repeating Timer carries the time it overshot into its next round, and [onFinished] may start or stop it again.
     */
    fun StringBuilder.appendTimerCountdown(timer: String, onFinished: StringBuilder.() -> Unit) {
        val remaining = TimerNaming.remainingField(timer)
        val period = TimerNaming.periodField(timer)
        appendLine("        if ($remaining > 0) {")
        appendLine("            $remaining -= deltaTimeInMilliseconds")
        appendLine("            if ($remaining <= 0) {")
        appendLine("                $remaining = if ($period > 0) ($remaining + $period).coerceAtLeast(1) else 0")
        onFinished()
        appendLine("            }")
        appendLine("        }")
    }

    /**
     * The velocity of a speed in a direction, for the Move in a direction Block. A function rather than lines of the
     * Block, so the direction and the speed are each read once, with no local of the Block in scope.
     *
     * @param visibility `private` for a type of its own, `protected` for a family's base class.
     */
    fun StringBuilder.appendDirectionFunction(visibility: String) {
        appendLine()
        appendLine("    /**")
        appendLine("     * The velocity of [speed] towards [directionInDegrees], clockwise from the right (a degree is 0.017453292 radians).")
        appendLine("     */")
        appendLine("    $visibility fun $DIRECTION_FUNCTION(directionInDegrees: Float, speed: Float): SceneOffset {")
        appendLine("        val radians = directionInDegrees * 0.017453292f")
        appendLine("        return SceneOffset((cos(radians) * speed).sceneUnit, (sin(radians) * speed).sceneUnit)")
        appendLine("    }")
    }

    /**
     * The velocity of the Move with keys Block: a function rather than lines of the Block, so the speed is read once,
     * with no local of the Block in scope. Opposite keys cancel out, and a diagonal is as fast as a straight line.
     *
     * @param visibility `private` for a type of its own, `protected` for a family's base class.
     */
    fun StringBuilder.appendSteeringFunction(visibility: String) {
        fun held(arrow: String, letter: String) = "usesArrows && keyboardInputManager.isKeyPressed(Key.$arrow) || usesWasd && keyboardInputManager.isKeyPressed(Key.$letter)"
        appendLine()
        appendLine("    /**")
        appendLine("     * The velocity of [speed] towards the arrow keys ([usesArrows]) and WASD keys ([usesWasd]) held: opposite keys cancel")
        appendLine("     * out, a diagonal is as fast as a straight line (0.70710677 is the square root of a half), and without")
        appendLine("     * [movesDiagonally] left and right win over up and down.")
        appendLine("     */")
        appendLine("    $visibility fun $STEERING_FUNCTION(speed: Float, usesArrows: Boolean, usesWasd: Boolean, movesDiagonally: Boolean): SceneOffset {")
        appendLine("        val isLeftHeld = ${held("DirectionLeft", "A")}")
        appendLine("        val isRightHeld = ${held("DirectionRight", "D")}")
        appendLine("        val isUpHeld = ${held("DirectionUp", "W")}")
        appendLine("        val isDownHeld = ${held("DirectionDown", "S")}")
        appendLine("        val x = (if (isRightHeld) 1f else 0f) - (if (isLeftHeld) 1f else 0f)")
        appendLine("        val y = if (x != 0f && !movesDiagonally) 0f else (if (isDownHeld) 1f else 0f) - (if (isUpHeld) 1f else 0f)")
        appendLine("        val step = if (x != 0f && y != 0f) speed * 0.70710677f else speed")
        appendLine("        return SceneOffset((x * step).sceneUnit, (y * step).sceneUnit)")
        appendLine("    }")
    }

    /**
     * Turns a number into text without its ".0" when it is a whole number, for [NUMBER_TEXT_FUNCTION].
     */
    fun StringBuilder.appendNumberTextFunction(visibility: String) {
        appendLine()
        appendLine("    $visibility fun Float.$NUMBER_TEXT_FUNCTION() = if (this % 1f == 0f) toLong().toString() else toString()")
    }

    /**
     * Keeps a Dynamic Actor updating while it is far outside the screen, as GameMaker's instances do: a Scene can be
     * larger than the screen, and its Timers, spawners, and chasers have to keep working there.
     */
    fun StringBuilder.appendAlwaysActive() {
        appendLine()
        appendLine("    override val isAlwaysActive = true")
    }

    /**
     * Brings the collision mask in line with the body: its position, and for Actors that may change their rotation
     * or size in Kotlin Blocks, the box's rotation or the circle's radius (both cheap when they didn't change).
     *
     * @param shapes The shapes the Actors may have: one for a type of its own, any of them for a family's base class.
     */
    fun StringBuilder.appendCollisionMaskUpdate(shapes: Set<ActorShape>) {
        appendLine("        collisionMask.position = bodyCenter")
        if (shapes.size == 1) {
            appendLine("        ${maskUpdate(shapes.single(), "collisionMask")}")
        } else {
            appendLine("        when (val mask = collisionMask) {")
            shapes.sorted().forEach { shape -> appendLine("            is ${maskClassOf(shape)} -> ${maskUpdate(shape, "mask")}") }
            appendLine("            else -> Unit")
            appendLine("        }")
        }
    }

    private fun maskUpdate(shape: ActorShape, mask: String) = when (shape) {
        ActorShape.CIRCLE -> "$mask.radius = body.size.width * body.scale.horizontal / 2"
        ActorShape.RECTANGLE -> "$mask.rotation = body.rotation"
    }

    fun maskClassOf(shape: ActorShape) = when (shape) {
        ActorShape.CIRCLE -> "CircleCollisionMask"
        ActorShape.RECTANGLE -> "BoxCollisionMask"
    }

    /**
     * The type a family's base classes keep the collision mask as: its shape's own when every Actor type in the family has
     * one shape, since [appendCollisionMaskUpdate] then sets that shape's members without checking.
     */
    fun maskTypeOf(shapes: Set<ActorShape>) = shapes.singleOrNull()?.let(::maskClassOf) ?: COLLISION_MASK

    const val COLLISION_MASK = "CollisionMask"

    /**
     * Turns the velocity away from the other Actor, on the axis they overlap less on, for the Bounce off it Block.
     *
     * @param visibility `private` for a type of its own, `protected` for a family's base class.
     */
    fun StringBuilder.appendBounceOffFunction(visibility: String) {
        appendLine()
        appendLine("    /**")
        appendLine("     * Turns the velocity away from [other], on the axis they overlap less on.")
        appendLine("     */")
        appendLine("    $visibility fun $BOUNCE_OFF_FUNCTION(other: Visible) {")
        appendLine("        val offset = body.position - other.body.position")
        appendLine("        if (offset.x.raw.absoluteValue * (body.size.height + other.body.size.height).raw >= offset.y.raw.absoluteValue * (body.size.width + other.body.size.width).raw) {")
        appendLine("            velocity = SceneOffset(if (offset.x > SceneUnit.Zero) velocity.x.raw.absoluteValue.sceneUnit else -velocity.x.raw.absoluteValue.sceneUnit, velocity.y)")
        appendLine("        } else {")
        appendLine("            velocity = SceneOffset(velocity.x, if (offset.y > SceneUnit.Zero) velocity.y.raw.absoluteValue.sceneUnit else -velocity.y.raw.absoluteValue.sceneUnit)")
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * Orders collision targets so that a descendant comes before its ancestors: `is MonsterBase` would catch a `Zombie`
     * too, so the more specific Event has to be asked first.
     */
    fun collisionTargetOrder(family: ActorTypeFamily) = compareByDescending<String> { family.ancestorsOf(it).size }.thenBy { it }
}
