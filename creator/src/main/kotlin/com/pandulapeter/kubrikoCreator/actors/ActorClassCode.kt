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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.BOUNCE_OFF_FUNCTION
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.SCENES_FIELD

/**
 * The parts of a generated Actor class that a type of its own ([ActorTypeCodeGenerator]) and the base class of a family
 * ([ActorFamilyCodeGenerator]) share, so that the two behave the same.
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

    private fun maskClassOf(shape: ActorShape) = when (shape) {
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
     * Keeps the Actor's bounding box inside the Scene's bounds (or the screen's), and runs [onTouched] when it touched
     * one of their edges, with `isTouchingLeftEdge` and the others in scope. The bounding box, rather than the body's
     * size around its position, so that a pivot that isn't in the middle, a rotation, or a scale don't move the edges.
     */
    fun StringBuilder.appendEdgeHandling(onTouched: StringBuilder.() -> Unit) {
        appendLine("        val box = body.axisAlignedBoundingBox")
        appendLine("        val sceneSettings = $SCENES_FIELD.settings")
        appendLine("        val topLeft = sceneSettings?.topLeft ?: viewportManager.topLeft.value")
        appendLine("        val bottomRight = sceneSettings?.bottomRight ?: viewportManager.bottomRight.value")
        // Strict, so an Actor resting where the push below left it doesn't run the Event again every frame.
        appendLine("        val isTouchingLeftEdge = box.min.x < topLeft.x")
        appendLine("        val isTouchingRightEdge = box.max.x > bottomRight.x")
        appendLine("        val isTouchingTopEdge = box.min.y < topLeft.y")
        appendLine("        val isTouchingBottomEdge = box.max.y > bottomRight.y")
        appendLine("        if (isTouchingLeftEdge || isTouchingRightEdge || isTouchingTopEdge || isTouchingBottomEdge) {")
        appendLine("            body.position += SceneOffset(")
        appendLine("                if (isTouchingLeftEdge) topLeft.x - box.min.x else if (isTouchingRightEdge) bottomRight.x - box.max.x else SceneUnit.Zero,")
        appendLine("                if (isTouchingTopEdge) topLeft.y - box.min.y else if (isTouchingBottomEdge) bottomRight.y - box.max.y else SceneUnit.Zero,")
        appendLine("            )")
        onTouched()
        appendLine("        }")
    }

    /**
     * Orders collision targets so that a descendant comes before its ancestors: `is MonsterBase` would catch a `Zombie`
     * too, so the more specific Event has to be asked first.
     */
    fun collisionTargetOrder(family: ActorTypeFamily) = compareByDescending<String> { family.ancestorsOf(it).size }.thenBy { it }
}
