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
import com.pandulapeter.kubrikoCreator.actors.AnimationNaming

/**
 * A collision mask class the generated code builds, by the name the code uses for it: the oval's is named in full rather
 * than imported, since an Actor type may be called `EllipseCollisionMask`, and reserving the name would need a migration
 * renaming Actor types (no Property may be called [AnimationNaming.PACKAGE_ROOT]).
 */
internal enum class MaskKind(val className: String, val isImported: Boolean = true) {
    CIRCLE("CircleCollisionMask"),
    ELLIPSE("${AnimationNaming.PACKAGE_ROOT}.pandulapeter.kubriko.collision.mask.EllipseCollisionMask", isImported = false),
    BOX("BoxCollisionMask"),
}

/**
 * An Actor type's body as its collision mask sees it: its shape, and whether it is as wide as it is high.
 */
internal data class MaskBody(val shape: ActorShape, val isSquare: Boolean) {
    companion object {
        fun of(definition: ActorTypeDefinition) = MaskBody(definition.shape, definition.width == definition.height)
    }
}

/**
 * The collision masks of the generated Actor classes: which class each one is, how it is built, and how the update keeps
 * it in line with the body. A round Actor collides as a circle, which is exact and cheapest, unless it may not be round:
 * one whose type isn't square, or whose size Blocks or Kotlin may make it so, is an oval ([MaskKind.ELLIPSE]) from the
 * start, and one placed stretched in a Scene picks the oval as it is created. A Kubriko without ovals keeps every round
 * Actor a circle, as an oval wouldn't build there.
 */
internal object CollisionMaskCode {
    const val IMPORT_PREFIX = "com.pandulapeter.kubriko.collision.mask."
    const val COLLISION_MASK = "CollisionMask"

    /**
     * The mask classes an Actor of [body] may have, given whether the project's Kubriko [hasOvalMasks] and whether the
     * Blocks may make a round Actor oval ([mayBecomeOval]).
     */
    fun kindsOf(body: MaskBody, hasOvalMasks: Boolean, mayBecomeOval: Boolean): Set<MaskKind> = when (body.shape) {
        ActorShape.RECTANGLE -> setOf(MaskKind.BOX)
        ActorShape.CIRCLE -> when {
            !hasOvalMasks -> setOf(MaskKind.CIRCLE)
            mayBecomeOval || !body.isSquare -> setOf(MaskKind.ELLIPSE)
            else -> setOf(MaskKind.CIRCLE, MaskKind.ELLIPSE)
        }
    }

    /**
     * The type a class keeps the mask as: its own class when it can be only one, since [appendCollisionMaskUpdate] then
     * sets its members without checking.
     */
    fun typeOf(kinds: Set<MaskKind>) = kinds.singleOrNull()?.className ?: COLLISION_MASK

    fun importsOf(kinds: Set<MaskKind>) = kinds.filter { it.isImported }.map { IMPORT_PREFIX + it.className } +
        listOfNotNull((IMPORT_PREFIX + COLLISION_MASK).takeIf { kinds.size > 1 })

    /**
     * The mask property of a type of its own, built around [center] from its `body`.
     */
    fun declarationOf(kinds: Set<MaskKind>, center: String): String = when (val single = kinds.singleOrNull()) {
        null -> buildString {
            appendLine("    override val collisionMask: $COLLISION_MASK = if (${isRound("body")}) {")
            appendConstruction(MaskKind.CIRCLE, "body", center, indent = "        ")
            appendLine("    } else {")
            appendConstruction(MaskKind.ELLIPSE, "body", center, indent = "        ")
            append("    }")
        }
        else -> buildString {
            append("    override val collisionMask = ")
            appendConstruction(single, "body", center, indent = "    ", isContinued = true)
        }.trimEnd()
    }

    /**
     * The mask a family's final class hands its base class, as one expression built from [body].
     */
    fun expressionOf(kinds: Set<MaskKind>, body: String, center: String): String = when (val single = kinds.singleOrNull()) {
        null -> "if (${isRound(body)}) ${inline(MaskKind.CIRCLE, body, center)} else ${inline(MaskKind.ELLIPSE, body, center)}"
        else -> inline(single, body, center)
    }

    /**
     * Brings the collision mask in line with the body: its position, the circle's radius, and the box's or oval's
     * rotation, and, when [followsSize], its size, each of which the mask changes in place and only when it differs. A mask
     * changed in place is what the collision plugin's grids and a physics body hold, which a new one wouldn't be.
     */
    fun StringBuilder.appendCollisionMaskUpdate(kinds: Set<MaskKind>, followsSize: Boolean) {
        appendLine("        collisionMask.position = bodyCenter")
        if (kinds.size == 1) {
            updateOf(kinds.single(), "collisionMask", followsSize).forEach { appendLine("        $it") }
        } else {
            appendLine("        when (val mask = collisionMask) {")
            kinds.sorted().forEach { kind ->
                val lines = updateOf(kind, "mask", followsSize)
                if (lines.size == 1) {
                    appendLine("            is ${kind.className} -> ${lines.single()}")
                } else {
                    appendLine("            is ${kind.className} -> {")
                    lines.forEach { appendLine("                $it") }
                    appendLine("            }")
                }
            }
            appendLine("            else -> Unit")
            appendLine("        }")
        }
    }

    private fun updateOf(kind: MaskKind, mask: String, followsSize: Boolean) = when (kind) {
        MaskKind.CIRCLE -> listOf("$mask.radius = body.size.width * body.scale.horizontal / 2")
        MaskKind.ELLIPSE, MaskKind.BOX -> listOfNotNull("$mask.rotation = body.rotation", "$mask.size = body.size * body.scale".takeIf { followsSize })
    }

    private fun isRound(body: String) = "$body.size.width * $body.scale.horizontal == $body.size.height * $body.scale.vertical"

    private fun argumentsOf(kind: MaskKind, body: String, center: String) = when (kind) {
        MaskKind.CIRCLE -> listOf("initialPosition = $center", "initialRadius = $body.size.width * $body.scale.horizontal / 2")
        MaskKind.ELLIPSE, MaskKind.BOX -> listOf("initialPosition = $center", "initialSize = $body.size * $body.scale", "initialRotation = $body.rotation")
    }

    private fun inline(kind: MaskKind, body: String, center: String) = argumentsOf(kind, body, center).joinToString(prefix = "${kind.className}(", postfix = ")")

    private fun StringBuilder.appendConstruction(kind: MaskKind, body: String, center: String, indent: String, isContinued: Boolean = false) {
        if (!isContinued) append(indent)
        appendLine("${kind.className}(")
        argumentsOf(kind, body, center).forEach { appendLine("$indent    $it,") }
        appendLine("$indent)")
    }
}
