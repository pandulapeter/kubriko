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
    const val SCENES_FIELD = "generatedScenes"
    const val AUDIO_FIELD = "generatedAudio"
    const val STATE_MANAGER_FIELD = "stateManager"
    const val STATE_MANAGER_IMPORT = "com.pandulapeter.kubriko.manager.StateManager"

    /**
     * Whether any of [blocks] loads a Scene, through the generated [ScenesCodeGenerator.SCENES_NAME] Manager.
     */
    fun usesScenes(blocks: List<BlockDefinition>) = blocks.any { (it is BlockDefinition.GoToScene && it.scene.isNotEmpty()) || it == BlockDefinition.RestartScene }

    /**
     * Whether any of [blocks] pauses or resumes the game, or checks whether it is paused, through `StateManager`.
     */
    fun usesPause(blocks: List<BlockDefinition>) = blocks.any { it is BlockDefinition.PauseGame || (it as? BlockDefinition.If)?.condition is ConditionDefinition.Paused }

    /**
     * Whether any of [blocks] plays a sound or music, or stops the music, through the generated
     * [AudioCodeGenerator.AUDIO_NAME] Manager.
     */
    fun usesAudio(blocks: List<BlockDefinition>) = blocks.any { it.audioFile != null || it == BlockDefinition.StopMusic }

    internal fun StringBuilder.appendEvents(events: List<EventDefinition>, depth: Int, scope: BlockScope) {
        val lines = events.flatMap { event -> eventLines(event.blocks, event.type, scope) }
        if (lines.isNotEmpty() && events.first().type != EventType.KEY_PRESSED && events.first().type != EventType.COLLISION && events.first().type != EventType.TIMER_FINISHED) {
            appendLine(INDENT.repeat(depth) + "// " + eventComment(events.first().type))
        }
        lines.forEach { appendLine(INDENT.repeat(depth) + it) }
    }

    internal fun eventComment(type: EventType) = when (type) {
        EventType.GAME_STARTED -> "When the game starts"
        EventType.ADDED_TO_SCENE -> "When added to the Scene"
        EventType.EVERY_FRAME -> "Every frame"
        EventType.KEY_PRESSED -> "When a key is pressed"
        EventType.EDGE_TOUCHED -> "When an edge of the screen is touched"
        EventType.COLLISION -> "When it collides with another Actor"
        EventType.TIMER_FINISHED -> "When a Timer finishes"
    }

    /**
     * Counts Timer [timer] down and runs the Blocks of its [events] when it finishes. A repeating Timer carries the time
     * it overshot into its next round, and the Blocks may start or stop it again.
     */
    internal fun StringBuilder.appendTimer(timer: Int, events: List<EventDefinition>, scope: BlockScope) {
        val remaining = remainingField(timer)
        val period = periodField(timer)
        appendLine("        if ($remaining > 0) {")
        appendLine("            $remaining -= deltaTimeInMilliseconds")
        appendLine("            if ($remaining <= 0) {")
        appendLine("                // When Timer $timer finishes")
        appendLine("                $remaining = if ($period > 0) ($remaining + $period).coerceAtLeast(1) else 0")
        appendEvents(events, depth = 4, scope = scope)
        appendLine("            }")
        appendLine("        }")
    }

    /**
     * @param guard A statement that runs before any of the Events, like an early return.
     */
    internal fun StringBuilder.appendKeyHandling(keyEvents: List<EventDefinition>, scope: BlockScope, guard: String? = null) {
        appendLine()
        appendLine("    override fun onKeyPressed(key: Key) {")
        guard?.let { appendLine("        $it") }
        appendLine("        when (key) {")
        keyEvents.groupBy { it.key!! }.toSortedMap().forEach { (key, events) ->
            appendLine("            Key.${key.composeKeyName} -> {")
            appendEvents(events, depth = 4, scope = scope)
            appendLine("            }")
        }
        appendLine("        }")
        appendLine("    }")
    }

    /**
     * The statements of an Event of [eventType] that runs [blocks]: their sections become Kotlin blocks, closed at the end
     * of the Event if they aren't before, and else or end Blocks that belong to no section become comments.
     */
    internal fun eventLines(
        blocks: List<BlockDefinition>,
        eventType: EventType,
        scope: BlockScope,
    ): List<String> {
        val (nesting, openSections) = BlockNesting.of(blocks)
        return blocks.indices.flatMap { index ->
            val indent = INDENT.repeat(nesting[index].depth)
            when (nesting[index].role) {
                BlockNesting.Role.ELSE -> listOf("$indent} else {")
                BlockNesting.Role.CLOSES -> listOf("$indent}")
                BlockNesting.Role.STRAY -> listOf(indent + if (blocks[index] == BlockDefinition.Else) "// Else only works once, after an if Block." else "// End only works after an if or repeat Block.")
                BlockNesting.Role.PLAIN, BlockNesting.Role.OPENS -> blockLines(blocks[index], eventType, scope).map { indent + it }
            }
        } + (openSections - 1 downTo 0).map { depth -> INDENT.repeat(depth) + "}" }
    }

    /**
     * The statements of [block] in an Event of [eventType]. Blocks that need what only one Event knows become a comment
     * anywhere else, and so do Blocks that need an Actor in a Manager type, so that a definition edited by hand still
     * compiles.
     */
    internal fun blockLines(
        block: BlockDefinition,
        eventType: EventType,
        scope: BlockScope,
    ): List<String> = if (BlockKind.of(block).needsActor && scope.kind != BlockTypeKind.ACTOR) {
        listOf("// This Block only works in an Actor type.")
    } else {
        actorBlockLines(block, eventType, scope)
    }

    private fun actorBlockLines(
        block: BlockDefinition,
        eventType: EventType,
        scope: BlockScope,
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
        is BlockDefinition.SetNumber -> propertyLines(block, scope) { "$it = ${floatLiteral(block.value)}" }
        is BlockDefinition.ChangeNumber -> propertyLines(block, scope) { "$it += ${floatLiteral(block.by)}" }
        is BlockDefinition.SetText -> propertyLines(block, scope) { "$it = ${stringLiteral(block.value)}" }
        is BlockDefinition.SetToggle -> propertyLines(block, scope) { "$it = ${block.value}" }
        is BlockDefinition.AddActor -> if (block.actorType in scope.actorTypeNames) {
            val position = offsetLiteral(block.x, block.y)
            listOf("actorManager.add(${block.actorType}.metadata.instantiate(${if (block.isRelative && scope.kind == BlockTypeKind.ACTOR) "body.position + $position" else position}).restore())")
        } else {
            listOf("// Choose an Actor type for this Block.")
        }
        is BlockDefinition.MoveTowardsNearest -> if (block.actorType in scope.actorTypeNames) {
            listOf(
                "run {",
                "    val actors = actorManager.allActors.value",
                "    var nearest: ${block.actorType}? = null",
                "    for (index in actors.indices) {",
                "        val candidate = actors[index] as? ${block.actorType} ?: continue",
                "        if (${if (block.actorType == scope.typeName) "candidate !== this && " else ""}(nearest == null || body.position.distanceTo(candidate.body.position) < body.position.distanceTo(nearest.body.position))) {",
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
        is BlockDefinition.GoToScene -> if (block.scene.isNotEmpty()) listOf("$SCENES_FIELD.goTo(${stringLiteral(block.scene)})") else listOf("// Choose a Scene for this Block.")
        BlockDefinition.RestartScene -> listOf("$SCENES_FIELD.restart()")
        is BlockDefinition.PauseGame -> listOf(
            "$STATE_MANAGER_FIELD.updateIsRunning(" + when (block.action) {
                PauseAction.PAUSE -> "false"
                PauseAction.RESUME -> "true"
                PauseAction.TOGGLE -> "!$STATE_MANAGER_FIELD.isRunning.value"
            } + ")",
        )
        is BlockDefinition.PlaySound -> if (block.sound.isNotEmpty()) listOf("$AUDIO_FIELD.playSound(${stringLiteral(block.sound)})") else listOf("// Choose a sound for this Block.")
        is BlockDefinition.PlayMusic -> if (block.music.isNotEmpty()) {
            listOf("$AUDIO_FIELD.playMusic(${stringLiteral(block.music)}${if (block.isLooping) "" else ", isLooping = false"})")
        } else {
            listOf("// Choose music for this Block.")
        }
        BlockDefinition.StopMusic -> listOf("$AUDIO_FIELD.stopMusic()")
        is BlockDefinition.If -> listOf("if (${conditionExpression(block.condition, scope)}) {")
        is BlockDefinition.Repeat -> listOf("repeat(${block.times.coerceAtLeast(0)}) {")
        BlockDefinition.Else, BlockDefinition.End -> emptyList()
        is BlockDefinition.Log -> listOf("Logger.log(message = ${templateLiteral(block.message, scope)}, source = ${stringLiteral(scope.typeName)})")
        // Each Kotlin Block gets a scope of its own, like GameMaker's code actions, so two of them can declare the same name.
        is BlockDefinition.Kotlin -> block.code.lines().map { it.trimEnd() }.dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let { lines -> listOf("run {") + lines.map { if (it.isEmpty()) it else INDENT + it } + "}" }
            .orEmpty()
    }

    /**
     * [condition] as a Kotlin expression, or `false` if it needs a Property that doesn't exist (of that kind), or an Actor
     * in a Manager type, so that the generated code always compiles.
     */
    private fun conditionExpression(condition: ConditionDefinition, scope: BlockScope): String {
        val reference = BlockDefinition.If(condition).propertyReference
        if (reference != null && !scope.hasProperty(reference)) return "false"
        if (!ConditionKind.of(condition).isAvailableIn(scope.kind)) return "false"
        val property = reference?.let { propertyExpression(it, scope) }
        return when (condition) {
            is ConditionDefinition.Number -> "$property ${condition.comparison.operator} ${floatLiteral(condition.value)}"
            is ConditionDefinition.Text -> "$property ${if (condition.isEqual) "==" else "!="} ${stringLiteral(condition.value)}"
            is ConditionDefinition.Toggle -> if (condition.isOn) "$property" else "!$property"
            is ConditionDefinition.KeyHeld -> (if (condition.isHeld) "" else "!") + "keyboardInputManager.isKeyPressed(Key.${condition.key.composeKeyName})"
            is ConditionDefinition.Velocity -> "velocity.${if (condition.axis == Axis.HORIZONTAL) "x" else "y"} ${condition.comparison.operator} ${unitLiteral(condition.value)}"
            is ConditionDefinition.Chance -> "Random.nextFloat() * 100f < ${floatLiteral(condition.percent)}"
            is ConditionDefinition.Paused -> (if (condition.isPaused) "!" else "") + "$STATE_MANAGER_FIELD.isRunning.value"
        }
    }

    /**
     * The statement of a Block that changes a Property, given the Property's expression, or a comment if there is no
     * Property of that name and kind (one that was renamed, deleted, or not chosen yet), so that the generated code
     * always compiles.
     */
    private fun propertyLines(block: BlockDefinition, scope: BlockScope, statement: (property: String) -> String): List<String> {
        val reference = checkNotNull(block.propertyReference)
        return if (scope.hasProperty(reference)) listOf(statement(propertyExpression(reference, scope))) else listOf("// Choose a Property for this Block.")
    }

    /**
     * A type's own Property by its name, and a Manager type's through the field the generated class keeps it in.
     */
    private fun propertyExpression(reference: PropertyReference, scope: BlockScope) = scope.ownerOf(reference.manager).let { owner ->
        if (owner == "this") reference.name else "$owner.${reference.name}"
    }

    /**
     * [text] as a string literal in which each placeholder ([PropertyPlaceholders]) is the value of its Property (a whole
     * number without its ".0", through [NUMBER_TEXT_FUNCTION]). Braces around anything else stay as they are.
     */
    internal fun templateLiteral(text: String, scope: BlockScope): String {
        var end = 0
        return buildString {
            append('"')
            PropertyPlaceholders.find(text).forEach { placeholder ->
                val property = scope.propertyOf(placeholder) ?: return@forEach
                appendEscaped(text.substring(end, placeholder.range.first))
                append("\${").append(propertyExpression(PropertyReference(placeholder.name, PropertyKind.of(property), placeholder.manager), scope))
                if (property is PropertyDefinition.Number) append(".$NUMBER_TEXT_FUNCTION()")
                append('}')
                end = placeholder.range.last + 1
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
