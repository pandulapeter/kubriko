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

import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Turns the Blocks of an Event into ordinary Kotlin statements, for every kind of block-built type.
 */
internal object BlockCodeGenerator {

    const val INDENT = "    "
    const val NUMBER_TEXT_FUNCTION = "asText"

    /**
     * The statements of an Event of [eventType] that runs [blocks]: their sections become Kotlin blocks, closed at the end
     * of the Event if they aren't before, and else or end Blocks that belong to no section become comments.
     */
    internal fun eventLines(
        blocks: List<BlockDefinition>,
        eventType: EventType,
        actorTypeName: String,
        properties: List<PropertyDefinition> = emptyList(),
        knownActorTypeNames: Collection<String> = listOf(actorTypeName),
    ): List<String> {
        val (nesting, openSections) = BlockNesting.of(blocks)
        return blocks.indices.flatMap { index ->
            val indent = INDENT.repeat(nesting[index].depth)
            when (nesting[index].role) {
                BlockNesting.Role.ELSE -> listOf("$indent} else {")
                BlockNesting.Role.CLOSES -> listOf("$indent}")
                BlockNesting.Role.STRAY -> listOf(indent + if (blocks[index] == BlockDefinition.Else) "// Else only works once, after an if Block." else "// End only works after an if or repeat Block.")
                BlockNesting.Role.PLAIN, BlockNesting.Role.OPENS -> blockLines(blocks[index], eventType, actorTypeName, properties, knownActorTypeNames).map { indent + it }
            }
        } + (openSections - 1 downTo 0).map { depth -> INDENT.repeat(depth) + "}" }
    }

    /**
     * The statements of [block] in an Event of [eventType]. Blocks that need what only one Event knows become a comment
     * anywhere else, so that a definition edited by hand still compiles.
     */
    internal fun blockLines(
        block: BlockDefinition,
        eventType: EventType,
        actorTypeName: String,
        properties: List<PropertyDefinition> = emptyList(),
        knownActorTypeNames: Collection<String> = listOf(actorTypeName),
    ): List<String> = when (block) {
        is BlockDefinition.SetVelocity -> listOf("velocity = ${offsetLiteral(block.x, block.y)}")
        is BlockDefinition.AddToVelocity -> listOf("velocity += ${offsetLiteral(block.x, block.y)}")
        BlockDefinition.Stop -> listOf("velocity = SceneOffset.Zero")
        is BlockDefinition.JumpTo -> listOf("body.position = ${offsetLiteral(block.x, block.y)}")
        BlockDefinition.BounceOffEdge -> if (eventType == EventType.EDGE_TOUCHED) {
            listOf(
                "if (isTouchingLeftEdge) velocity = SceneOffset(velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingRightEdge) velocity = SceneOffset(-velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingTopEdge) velocity = SceneOffset(velocity.x, velocity.y.raw.absoluteValue.sceneUnit)",
                "if (isTouchingBottomEdge) velocity = SceneOffset(velocity.x, -velocity.y.raw.absoluteValue.sceneUnit)",
            )
        } else {
            listOf("// Bounce off the edge only works in the Edge touched Event.")
        }
        BlockDefinition.RemoveSelf -> listOf("actorManager.remove(this)")
        BlockDefinition.BounceOffOther -> if (eventType == EventType.COLLISION) {
            listOf(
                "run {",
                "    val offset = body.position - other.body.position",
                "    if (offset.x.raw.absoluteValue * (body.size.height + other.body.size.height).raw >= offset.y.raw.absoluteValue * (body.size.width + other.body.size.width).raw) {",
                "        velocity = SceneOffset(if (offset.x > SceneUnit.Zero) velocity.x.raw.absoluteValue.sceneUnit else -velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "    } else {",
                "        velocity = SceneOffset(velocity.x, if (offset.y > SceneUnit.Zero) velocity.y.raw.absoluteValue.sceneUnit else -velocity.y.raw.absoluteValue.sceneUnit)",
                "    }",
                "}",
            )
        } else {
            listOf("// Bounce off it only works in a collision Event.")
        }
        BlockDefinition.RemoveOther -> if (eventType == EventType.COLLISION) {
            listOf("actorManager.remove(other)")
        } else {
            listOf("// Remove the other Actor only works in a collision Event.")
        }
        is BlockDefinition.SetNumber -> propertyLines(block, properties) { "${block.property} = ${floatLiteral(block.value)}" }
        is BlockDefinition.ChangeNumber -> propertyLines(block, properties) { "${block.property} += ${floatLiteral(block.by)}" }
        is BlockDefinition.SetText -> propertyLines(block, properties) { "${block.property} = ${stringLiteral(block.value)}" }
        is BlockDefinition.SetToggle -> propertyLines(block, properties) { "${block.property} = ${block.value}" }
        is BlockDefinition.AddActor -> if (block.actorType in knownActorTypeNames) {
            val position = offsetLiteral(block.x, block.y)
            listOf("actorManager.add(${block.actorType}.metadata.instantiate(${if (block.isRelative) "body.position + $position" else position}).restore())")
        } else {
            listOf("// Choose an Actor type for this Block.")
        }
        is BlockDefinition.MoveTowardsNearest -> if (block.actorType in knownActorTypeNames) {
            listOf(
                "run {",
                "    val actors = actorManager.allActors.value",
                "    var nearest: ${block.actorType}? = null",
                "    for (index in actors.indices) {",
                "        val candidate = actors[index] as? ${block.actorType} ?: continue",
                "        if (${if (block.actorType == actorTypeName) "candidate !== this && " else ""}(nearest == null || body.position.distanceTo(candidate.body.position) < body.position.distanceTo(nearest.body.position))) {",
                "            nearest = candidate",
                "        }",
                "    }",
                "    if (nearest != null) velocity = (nearest.body.position - body.position).normalized() * ${floatLiteral(block.speed)}",
                "}",
            )
        } else {
            listOf("// Choose an Actor type for this Block.")
        }
        is BlockDefinition.StartTimer -> if (block.timer in TIMER_NUMBERS) {
            val milliseconds = (block.seconds * 1000).roundToLong().coerceAtLeast(1)
            listOf("${remainingField(block.timer)} = $milliseconds", "${periodField(block.timer)} = ${if (block.isRepeating) milliseconds else 0}")
        } else {
            listOf("// Start a Timer only works with Timers 1 to 8.")
        }
        is BlockDefinition.StopTimer -> if (block.timer in TIMER_NUMBERS) listOf("${remainingField(block.timer)} = 0") else listOf("// Stop a Timer only works with Timers 1 to 8.")
        is BlockDefinition.If -> listOf("if (${conditionExpression(block.condition, properties)}) {")
        is BlockDefinition.Repeat -> listOf("repeat(${block.times.coerceAtLeast(0)}) {")
        BlockDefinition.Else, BlockDefinition.End -> emptyList()
        is BlockDefinition.Log -> listOf("Logger.log(message = ${templateLiteral(block.message, properties)}, source = ${stringLiteral(actorTypeName)})")
        // Each Kotlin Block gets a scope of its own, like GameMaker's code actions, so two of them can declare the same name.
        is BlockDefinition.Kotlin -> block.code.lines().map { it.trimEnd() }.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let { lines -> listOf("run {") + lines.map { if (it.isEmpty()) it else INDENT + it } + "}" }
            .orEmpty()
    }

    /**
     * [condition] as a Kotlin expression, or `false` if it needs a Property the Actor type doesn't have (of that kind),
     * so that the generated code always compiles.
     */
    private fun conditionExpression(condition: ConditionDefinition, properties: List<PropertyDefinition>): String {
        val reference = BlockDefinition.If(condition).propertyReference
        if (reference != null && properties.none { it.name == reference.first && PropertyKind.of(it) == reference.second }) return "false"
        return when (condition) {
            is ConditionDefinition.Number -> "${condition.property} ${condition.comparison.operator} ${floatLiteral(condition.value)}"
            is ConditionDefinition.Text -> "${condition.property} ${if (condition.isEqual) "==" else "!="} ${stringLiteral(condition.value)}"
            is ConditionDefinition.Toggle -> if (condition.isOn) condition.property else "!${condition.property}"
            is ConditionDefinition.KeyHeld -> (if (condition.isHeld) "" else "!") + "keyboardInputManager.isKeyPressed(Key.${condition.key.composeKeyName})"
            is ConditionDefinition.Velocity -> "velocity.${if (condition.axis == Axis.HORIZONTAL) "x" else "y"} ${condition.comparison.operator} ${unitLiteral(condition.value)}"
            is ConditionDefinition.Chance -> "Random.nextFloat() * 100f < ${floatLiteral(condition.percent)}"
        }
    }

    /**
     * The statement of a Block that changes a Property, or a comment if the Actor type has no Property of that name
     * and kind (one that was renamed, deleted, or not chosen yet), so that the generated code always compiles.
     */
    private fun propertyLines(block: BlockDefinition, properties: List<PropertyDefinition>, statement: () -> String): List<String> {
        val (name, kind) = checkNotNull(block.propertyReference)
        return if (properties.any { it.name == name && PropertyKind.of(it) == kind }) listOf(statement()) else listOf("// Choose a Property for this Block.")
    }

    /**
     * [text] as a string literal in which `{name}` is the value of the Property called name (a whole number without
     * its ".0", through [NUMBER_TEXT_FUNCTION]). Braces around anything else stay as they are.
     */
    internal fun templateLiteral(text: String, properties: List<PropertyDefinition>): String {
        val propertiesByName = properties.associateBy { it.name }
        var end = 0
        return buildString {
            append('"')
            propertyPlaceholder.findAll(text).filter { it.groupValues[1] in propertiesByName }.forEach { match ->
                val name = match.groupValues[1]
                appendEscaped(text.substring(end, match.range.first))
                append("\${").append(name)
                if (propertiesByName.getValue(name) is PropertyDefinition.Number) append(".$NUMBER_TEXT_FUNCTION()")
                append('}')
                end = match.range.last + 1
            }
            appendEscaped(text.substring(end))
            append('"')
        }
    }

    internal fun stringLiteral(text: String) = buildString {
        append('"')
        appendEscaped(text)
        append('"')
    }

    private fun StringBuilder.appendEscaped(text: String) {
        text.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    internal fun initialValueLiteral(property: PropertyDefinition) = when (property) {
        is PropertyDefinition.Number -> floatLiteral(property.initialValue)
        is PropertyDefinition.Text -> stringLiteral(property.initialValue)
        is PropertyDefinition.Toggle -> property.initialValue.toString()
    }

    private val propertyPlaceholder = Regex("\\{([a-z][A-Za-z0-9]*)\\}")

    private fun offsetLiteral(x: Float, y: Float) = "SceneOffset(${unitLiteral(x)}, ${unitLiteral(y)})"

    internal fun unitLiteral(value: Float) = "${floatLiteral(value)}.sceneUnit"

    internal fun floatLiteral(value: Float) = when {
        !value.isFinite() -> "0f"
        value == value.toLong().toFloat() && value.absoluteValue < 1e9f -> "${value.toLong()}f"
        else -> "${value}f"
    }

    internal fun colorLiteral(color: Long) = "0x" + (color and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

    internal fun remainingField(timer: Int) = "timer${timer}RemainingInMilliseconds"

    internal fun periodField(timer: Int) = "timer${timer}PeriodInMilliseconds"
}
