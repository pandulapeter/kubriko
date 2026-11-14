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

import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers
import com.pandulapeter.kubrikoCreator.actors.KotlinUi
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PauseAction
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyPlaceholders
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.numberValues
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.readsEdges
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Turns the Blocks of an Event into ordinary Kotlin statements, for every kind of block-built type.
 */
public object BlockCodeGenerator {

    internal const val INDENT = "    "
    internal const val NUMBER_TEXT_FUNCTION = "asText"
    internal const val SCENES_FIELD = "generatedScenes"
    internal const val AUDIO_FIELD = "generatedAudio"
    internal const val STATE_MANAGER_FIELD = "stateManager"
    internal const val REMOVED_FIELD = "isRemoved"
    internal const val BOUNCE_OFF_FUNCTION = "bounceOff"
    internal const val DIRECTION_FUNCTION = "velocityInDirection"
    internal const val STEERING_FUNCTION = "velocityFromKeys"
    internal const val OPACITY_FIELD = "opacity"
    internal const val ANIMATION_FIELD = "animation"
    internal const val ANIMATION_SPEED_FIELD = "animationSpeed"
    internal const val ANIMATION_LOOPING_FIELD = "isAnimationLooping"
    internal const val SET_FRAME_FUNCTION = "setFrame"
    internal const val STEP_ANIMATION_FUNCTION = "stepAnimation"
    internal const val STATE_MANAGER_IMPORT = "com.pandulapeter.kubriko.manager.StateManager"
    internal const val ABSOLUTE_VALUE_IMPORT = "kotlin.math.absoluteValue"
    internal const val SCENE_OFFSET_IMPORT = "com.pandulapeter.kubriko.types.SceneOffset"
    private const val ABSOLUTE_VALUE = "absoluteValue"
    private const val SCENE_UNIT_EXTENSION_IMPORT = "com.pandulapeter.kubriko.helpers.extensions.sceneUnit"
    private const val NORMALIZED_IMPORT = "com.pandulapeter.kubriko.helpers.extensions.normalized"
    private const val LOGGER_IMPORT = "com.pandulapeter.kubriko.logger.Logger"
    private const val KEY_IMPORT = "androidx.compose.ui.input.key.Key"
    private const val RANDOM_IMPORT = "kotlin.random.Random"
    private const val RANDOM = "Random"

    /**
     * The statements of an Event of [eventType] that runs [blocks]: their sections become Kotlin blocks, closed at the end
     * of the Event if they aren't before, and else or end Blocks that belong to no section become comments.
     */
    internal fun eventCode(
        blocks: List<BlockDefinition>,
        eventType: EventType,
        scope: BlockScope,
    ): BlockCode {
        val (nesting, openSections) = BlockNesting.of(blocks)
        return blocks.indices.fold(BlockCode.NONE) { code, index ->
            val depth = nesting[index].depth
            code + when (nesting[index].role) {
                BlockNesting.Role.ELSE -> BlockCode(listOf("} else {")).indented(depth)
                BlockNesting.Role.CLOSES -> BlockCode(listOf("}")).indented(depth)
                BlockNesting.Role.STRAY -> BlockCode(listOf(if (blocks[index] == BlockDefinition.Else) "// Else only works once, after an if Block." else "// End only works after an if or repeat Block.")).indented(depth)
                BlockNesting.Role.PLAIN, BlockNesting.Role.OPENS -> blockCode(blocks[index], eventType, scope).indented(depth)
            }
        } + BlockCode((openSections - 1 downTo 0).map { depth -> INDENT.repeat(depth) + "}" })
    }

    /**
     * The statements of [block] in an Event of [eventType]. Blocks that need what only one Event knows become a comment
     * anywhere else, and so do Blocks that need an Actor in a Manager type, so that a definition edited by hand still
     * compiles.
     */
    internal fun blockCode(
        block: BlockDefinition,
        eventType: EventType,
        scope: BlockScope,
    ): BlockCode {
        val kind = BlockKind.of(block)
        return when {
            kind.needsActor && scope.kind != BlockTypeKind.ACTOR -> BlockCode(listOf("// This Block only works in an Actor type."))
            kind.onlyIn != null && kind.onlyIn != eventType -> BlockCode(listOfNotNull(kind.misplacedComment))
            else -> emittedCode(block, scope).let { code ->
                val numberValues = block.numberValues
                code.copy(needs = code.needs + NumberValueCode.needsOf(numberValues), managers = code.managers + NumberValueCode.managersReadBy(numberValues, scope))
            }
        }
    }

    private fun emittedCode(
        block: BlockDefinition,
        scope: BlockScope,
    ): BlockCode = when (block) {
        is BlockDefinition.SetVelocity -> movementCode("velocity = ${offsetCode(block.x, block.y, scope)}")
        is BlockDefinition.AddToVelocity -> movementCode("velocity += ${offsetCode(block.x, block.y, scope)}")
        is BlockDefinition.MoveInDirection -> BlockCode(
            lines = listOf("velocity = $DIRECTION_FUNCTION(${NumberValueCode.float(block.direction, scope)}, ${NumberValueCode.float(block.speed, scope)})"),
            needs = setOf(BlockNeed.MOVEMENT, BlockNeed.DIRECTION),
        )
        is BlockDefinition.MoveWithKeys -> moveWithKeysCode(block, scope)
        BlockDefinition.Stop -> movementCode("velocity = SceneOffset.Zero")
        BlockDefinition.RunParentEvent -> BlockCode(listOf(scope.parentEventCall ?: "// Only an Actor type with a parent runs its parent's Event."))
        is BlockDefinition.JumpTo -> movementCode("body.position = ${offsetCode(block.x, block.y, scope)}")
        BlockDefinition.BounceOffEdge -> BlockCode(
            lines = listOf(
                "if (isTouchingLeftEdge) velocity = SceneOffset(velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingRightEdge) velocity = SceneOffset(-velocity.x.raw.absoluteValue.sceneUnit, velocity.y)",
                "if (isTouchingTopEdge) velocity = SceneOffset(velocity.x, velocity.y.raw.absoluteValue.sceneUnit)",
                "if (isTouchingBottomEdge) velocity = SceneOffset(velocity.x, -velocity.y.raw.absoluteValue.sceneUnit)",
            ),
            needs = setOf(BlockNeed.MOVEMENT),
            imports = setOf(ABSOLUTE_VALUE_IMPORT, SCENE_OFFSET_IMPORT),
        )
        BlockDefinition.RemoveSelf -> BlockCode(listOf("$REMOVED_FIELD = true", "actorManager.remove(this)"), needs = setOf(BlockNeed.ACTOR_MANAGER))
        BlockDefinition.BounceOffOther -> BlockCode(listOf("$BOUNCE_OFF_FUNCTION(other)"), needs = setOf(BlockNeed.MOVEMENT, BlockNeed.BOUNCE_OFF))
        BlockDefinition.RemoveOther -> BlockCode(listOf("other.$REMOVED_FIELD = true", "actorManager.remove(other)"), needs = setOf(BlockNeed.ACTOR_MANAGER))
        is BlockDefinition.SetNumber -> propertyCode(block, scope) { "$it = ${NumberValueCode.float(block.value, scope)}" }
        is BlockDefinition.ChangeNumber -> propertyCode(block, scope) { "$it += ${NumberValueCode.float(block.by, scope)}" }
        is BlockDefinition.SetText -> propertyCode(block, scope) { "$it = ${stringLiteral(block.value)}" }
        is BlockDefinition.SetToggle -> propertyCode(block, scope) { "$it = ${block.value}" }
        is BlockDefinition.AddActor -> addActorCode(block, scope)
        is BlockDefinition.MoveTowardsNearest -> moveTowardsNearestCode(block, scope)
        is BlockDefinition.StartTimer -> startTimerCode(block, scope)
        is BlockDefinition.StopTimer -> BlockCode(listOf(if (TimerNaming.isValid(block.timer)) "${TimerNaming.remainingField(block.timer)} = 0" else "// Give this Block's Timer a name made of letters and digits."))
        is BlockDefinition.GoToScene -> if (block.scene.isNotEmpty()) {
            BlockCode(listOf("$SCENES_FIELD.goTo(${stringLiteral(block.scene)})"), needs = setOf(BlockNeed.SCENES))
        } else {
            BlockCode(listOf("// Choose a Scene for this Block."))
        }
        BlockDefinition.RestartScene -> BlockCode(listOf("$SCENES_FIELD.restart()"), needs = setOf(BlockNeed.SCENES))
        is BlockDefinition.PauseGame -> BlockCode(
            lines = listOf(
                "$STATE_MANAGER_FIELD.updateIsRunning(" + when (block.action) {
                    PauseAction.PAUSE -> "false"
                    PauseAction.RESUME -> "true"
                    PauseAction.TOGGLE -> "!$STATE_MANAGER_FIELD.isRunning.value"
                } + ")",
            ),
            needs = setOf(BlockNeed.STATE_MANAGER),
        )
        is BlockDefinition.PlaySound -> if (block.sound.isNotEmpty()) {
            BlockCode(listOf("$AUDIO_FIELD.playSound(${stringLiteral(block.sound)})"), needs = setOf(BlockNeed.AUDIO))
        } else {
            BlockCode(listOf("// Choose a sound for this Block."))
        }
        is BlockDefinition.PlayMusic -> if (block.music.isNotEmpty()) {
            BlockCode(listOf("$AUDIO_FIELD.playMusic(${stringLiteral(block.music)}${if (block.isLooping) "" else ", isLooping = false"})"), needs = setOf(BlockNeed.AUDIO))
        } else {
            BlockCode(listOf("// Choose music for this Block."))
        }
        BlockDefinition.StopMusic -> BlockCode(listOf("$AUDIO_FIELD.stopMusic()"), needs = setOf(BlockNeed.AUDIO))
        is BlockDefinition.SetFrame -> animationCode(scope, BlockNeed.FRAME, "$SET_FRAME_FUNCTION(${NumberValueCode.whole(block.frame, scope)})")
        is BlockDefinition.SetAnimation -> animationCode(scope, BlockNeed.ANIMATION, "$ANIMATION_SPEED_FIELD = ${NumberValueCode.float(block.speed, scope)}", "$ANIMATION_LOOPING_FIELD = ${block.isLooping}")
        is BlockDefinition.SetColor -> AppearanceCode.setColor(block)
        is BlockDefinition.SetOpacity -> AppearanceCode.setOpacity(block, scope)
        is BlockDefinition.ChangeOpacity -> AppearanceCode.changeOpacity(block, scope)
        is BlockDefinition.Animate -> AnimateCode.animate(block, scope)
        is BlockDefinition.If -> conditionCode(block, scope).let { condition -> condition.copy(lines = listOf("if (${condition.lines.single()}) {")) }
        is BlockDefinition.Repeat -> BlockCode(listOf("repeat(${NumberValueCode.whole(block.times, scope)}) {"))
        BlockDefinition.Else, BlockDefinition.End -> BlockCode.NONE
        is BlockDefinition.Log -> logCode(block, scope)
        is BlockDefinition.Kotlin -> kotlinCode(block, scope)
    }

    private fun animationCode(scope: BlockScope, need: BlockNeed, vararg lines: String) = if (scope.isAnimated) {
        BlockCode(lines.toList(), needs = setOf(need))
    } else {
        BlockCode(listOf("// This Block only works in an Actor type whose sprite is cut into frames."))
    }

    /**
     * Starting a Timer for a fixed time is worked out here; any other time is worked out when the Block runs, in the
     * remaining field, which the period then copies. That rounds through `toLong`, which turns a Property that became NaN
     * into the shortest time rather than throwing as `roundToLong` would.
     */
    private fun startTimerCode(block: BlockDefinition.StartTimer, scope: BlockScope): BlockCode {
        if (!TimerNaming.isValid(block.timer)) return BlockCode(listOf("// Give this Block's Timer a name made of letters and digits."))
        val remaining = TimerNaming.remainingField(block.timer)
        val period = TimerNaming.periodField(block.timer)
        val seconds = block.seconds
        return if (seconds is NumberValue.Fixed) {
            val milliseconds = (seconds.value * 1000).roundToLong().coerceAtLeast(1)
            BlockCode(listOf("$remaining = $milliseconds", "$period = ${if (block.isRepeating) milliseconds else 0}"))
        } else {
            BlockCode(
                lines = listOf("$remaining = (${NumberValueCode.float(seconds, scope)} * 1000f + 0.5f).toLong().coerceAtLeast(1)", "$period = ${if (block.isRepeating) remaining else "0"}"),
            )
        }
    }

    private fun movementCode(line: String) = BlockCode(listOf(line), needs = setOf(BlockNeed.MOVEMENT), imports = setOf(SCENE_OFFSET_IMPORT))

    private fun addActorCode(block: BlockDefinition.AddActor, scope: BlockScope) = if (block.actorType in scope.actorTypeNames) {
        val isActor = scope.kind == BlockTypeKind.ACTOR
        val position = if (isActor) placementCode(block.x, block.y, block.isRelative, scope) else offsetCode(block.x, block.y, scope)
        BlockCode(
            lines = listOf("actorManager.add(${block.actorType}.metadata.instantiate($position).restore())"),
            needs = setOf(BlockNeed.ACTOR_MANAGER),
            imports = if (isActor) setOf(SCENE_OFFSET_IMPORT) else setOf(SCENE_OFFSET_IMPORT, SCENE_UNIT_EXTENSION_IMPORT),
        )
    } else {
        BlockCode(listOf("// Choose an Actor type for this Block."))
    }

    /**
     * A position in the Scene, or, if [isRelative], from this Actor's. An edge is where it is in the Scene either way, so
     * only the other coordinate is added to this Actor's.
     */
    internal fun placementCode(x: NumberValue, y: NumberValue, isRelative: Boolean, scope: BlockScope) = when {
        !isRelative -> offsetCode(x, y, scope)
        !x.readsEdges && !y.readsEdges -> "body.position + ${offsetCode(x, y, scope)}"
        else -> "SceneOffset(${relativeCoordinate(x, "x", scope)}, ${relativeCoordinate(y, "y", scope)})"
    }

    private fun relativeCoordinate(value: NumberValue, axis: String, scope: BlockScope) =
        if (value.readsEdges) NumberValueCode.sceneUnit(value, scope) else "body.position.$axis + ${NumberValueCode.sceneUnit(value, scope)}"

    /**
     * Which keys are held is read at the call, so only the Block's own keys are checked, and the speed is an argument,
     * read with no local of the Block in scope.
     */
    private fun moveWithKeysCode(block: BlockDefinition.MoveWithKeys, scope: BlockScope): BlockCode {
        fun held(arrow: String, letter: String) = listOfNotNull(arrow.takeIf { block.keys.usesArrows }, letter.takeIf { block.keys.usesWasd })
            .joinToString(" || ") { "keyboardInputManager.isKeyPressed(Key.$it)" }
        return BlockCode(
            lines = listOf(
                "velocity = $STEERING_FUNCTION(",
                "${INDENT}speed = ${NumberValueCode.float(block.speed, scope)},",
                "${INDENT}isLeftHeld = ${held("DirectionLeft", "A")},",
                "${INDENT}isRightHeld = ${held("DirectionRight", "D")},",
                "${INDENT}isUpHeld = ${held("DirectionUp", "W")},",
                "${INDENT}isDownHeld = ${held("DirectionDown", "S")},",
                "${INDENT}movesDiagonally = ${block.movesDiagonally},",
                ")",
            ),
            needs = setOf(BlockNeed.MOVEMENT, BlockNeed.KEYBOARD_INPUT_MANAGER, BlockNeed.STEERING),
            imports = setOf(KEY_IMPORT),
        )
    }

    /**
     * The speed is read first, while no local of the Block is in scope yet, so a Property named like one of them is
     * still the Property.
     */
    private fun moveTowardsNearestCode(block: BlockDefinition.MoveTowardsNearest, scope: BlockScope): BlockCode {
        if (block.actorType !in scope.actorTypeNames) return BlockCode(listOf("// Choose an Actor type for this Block."))
        val actorClass = scope.classOf(block.actorType)
        val isOwnType = block.actorType == scope.typeName || block.actorType in scope.ancestorNames
        return BlockCode(
            lines = listOf(
                "run {",
                "    val speed = ${NumberValueCode.float(block.speed, scope)}",
                "    val actors = actorManager.allActors.value",
                "    var nearest: $actorClass? = null",
                "    var nearestDistanceSquared = Float.MAX_VALUE",
                "    for (index in actors.indices) {",
                "        val candidate = actors[index] as? $actorClass ?: continue",
                "        if (${if (isOwnType) "candidate === this || " else ""}candidate.$REMOVED_FIELD) continue",
                "        val offset = candidate.body.position - body.position",
                "        val distanceSquared = offset.x.raw * offset.x.raw + offset.y.raw * offset.y.raw",
                "        if (distanceSquared < nearestDistanceSquared) {",
                "            nearest = candidate",
                "            nearestDistanceSquared = distanceSquared",
                "        }",
                "    }",
                "    if (nearest != null) velocity = (nearest.body.position - body.position).normalized() * speed",
                "}",
            ),
            needs = setOf(BlockNeed.MOVEMENT, BlockNeed.ACTOR_MANAGER),
            imports = setOf(NORMALIZED_IMPORT, SCENE_OFFSET_IMPORT),
        )
    }

    /**
     * Logging allocates on every call, so a released game, which leaves it off, doesn't pay for it. While Kubriko Creator
     * plays the game it also prints, on one line, since only the desktop's session reports what Kubriko's Logger holds,
     * and Creator reads what a game in the browser or on a phone prints instead.
     */
    private fun logCode(block: BlockDefinition.Log, scope: BlockScope): BlockCode {
        val message = templateLiteral(block.message, scope)
        return BlockCode(
            lines = listOf(
                "if (${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.LOGGING_FLAG}) ${message.code}.let { logged ->",
                "    Logger.log(message = logged, source = ${stringLiteral(scope.typeName)})",
                "    println(${stringLiteral("[${scope.typeName}] ")} + logged.replace('\\n', ' '))",
                "}",
            ),
            needs = if (message.usesNumberText) setOf(BlockNeed.NUMBER_TEXT) else emptySet(),
            imports = setOf(LOGGER_IMPORT),
            managers = message.managers,
        )
    }

    /**
     * Each Kotlin Block gets a scope of its own, like GameMaker's code actions, so two of them can declare the same name.
     * Its code may use anything an Actor has, so it gets all of it, and the type of its velocity is imported in an Actor
     * type's file. The import lines it starts with become imports of the file, as Kotlin UI's do ([KotlinUi]).
     */
    private fun kotlinCode(block: BlockDefinition.Kotlin, scope: BlockScope): BlockCode {
        val (imports, statements) = KotlinUi.splitImports(block.code)
        val lines = statements.dropWhile { it.isEmpty() }
        if (lines.isEmpty()) return BlockCode(emptyList(), imports = imports.toSet())
        return BlockCode(
            lines = listOf("run {") + lines.map { if (it.isEmpty()) it else INDENT + it } + "}",
            needs = setOf(BlockNeed.MOVEMENT, BlockNeed.ACTOR_MANAGER, BlockNeed.VIEWPORT_MANAGER),
            imports = imports.toSet() + setOfNotNull(
                SCENE_OFFSET_IMPORT.takeIf { scope.kind == BlockTypeKind.ACTOR },
                ABSOLUTE_VALUE_IMPORT.takeIf { KotlinIdentifiers.find(lines.joinToString("\n")).any { it.name == ABSOLUTE_VALUE } },
                randomImportFor(lines.joinToString("\n"), imports),
            ),
        )
    }

    /**
     * Kotlin's `Random` for [code] that names it without importing a `Random` of its own ([imports]): generated files
     * imported it for every chance condition until the Blocks' randomness moved to the wiring, and Kotlin written since
     * may rely on it.
     */
    internal fun randomImportFor(code: String, imports: Collection<String>) = RANDOM_IMPORT.takeIf {
        imports.none { it.substringAfterLast('.') == RANDOM || it.endsWith(" as $RANDOM") } &&
            KotlinIdentifiers.find(code).any { it.name == RANDOM && it.receiver == null }
    }

    /**
     * The condition of [block] as a Kotlin expression (the only line of the result), or `false` if it needs a Property
     * that doesn't exist (of that kind), an Actor type that doesn't, or an Actor in a Manager type, so that the generated
     * code always compiles.
     */
    private fun conditionCode(block: BlockDefinition.If, scope: BlockScope): BlockCode {
        val condition = block.condition
        val reference = block.propertyReference
        if (reference != null && !scope.hasProperty(reference)) return BlockCode(listOf("false"))
        if (!ConditionKind.of(condition).isAvailableIn(scope.kind)) return BlockCode(listOf("false"))
        val property = reference?.let { propertyExpression(it, scope) }
        val managers = managersReachedBy(reference?.manager, scope)
        return when (condition) {
            is ConditionDefinition.Number -> BlockCode(listOf("$property ${condition.comparison.operator} ${NumberValueCode.float(condition.value, scope)}"), managers = managers)
            is ConditionDefinition.Text -> BlockCode(listOf("$property ${if (condition.isEqual) "==" else "!="} ${stringLiteral(condition.value)}"), managers = managers)
            is ConditionDefinition.Toggle -> BlockCode(listOf(if (condition.isOn) "$property" else "!$property"), managers = managers)
            is ConditionDefinition.KeyHeld -> if (condition.key.isKnown) {
                BlockCode(
                    lines = listOf((if (condition.isHeld) "" else "!") + "keyboardInputManager.isKeyPressed(Key.${condition.key.composeKeyName})"),
                    needs = setOf(BlockNeed.KEYBOARD_INPUT_MANAGER),
                    imports = setOf(KEY_IMPORT),
                )
            } else {
                BlockCode(listOf((!condition.isHeld).toString()))
            }
            is ConditionDefinition.Velocity -> BlockCode(
                lines = listOf("velocity.${if (condition.axis == Axis.HORIZONTAL) "x" else "y"} ${condition.comparison.operator} ${NumberValueCode.sceneUnit(condition.value, scope)}"),
                needs = setOf(BlockNeed.MOVEMENT),
            )
            is ConditionDefinition.ActorCount -> if (condition.actorType in scope.actorTypeNames) {
                BlockCode(
                    lines = listOf("${ActorCountCode.countOf(condition.actorType)}.toFloat() ${condition.comparison.operator} ${NumberValueCode.float(condition.value, scope)}"),
                    needs = setOf(BlockNeed.SCENES),
                )
            } else {
                BlockCode(listOf("false"))
            }
            is ConditionDefinition.Chance -> BlockCode(listOf("${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.RANDOM_FIELD}.nextFloat() * 100f < ${NumberValueCode.float(condition.percent, scope)}"))
            is ConditionDefinition.Paused -> BlockCode(listOf((if (condition.isPaused) "!" else "") + "$STATE_MANAGER_FIELD.isRunning.value"), needs = setOf(BlockNeed.STATE_MANAGER))
        }
    }

    /**
     * The statement of a Block that changes a Property, given the Property's expression, or a comment if there is no
     * Property of that name and kind (one that was renamed, deleted, or not chosen yet), so that the generated code
     * always compiles.
     */
    private fun propertyCode(block: BlockDefinition, scope: BlockScope, statement: (property: String) -> String): BlockCode {
        val reference = checkNotNull(block.propertyReference)
        return if (scope.hasProperty(reference)) {
            BlockCode(listOf(statement(propertyExpression(reference, scope))), managers = managersReachedBy(reference.manager, scope))
        } else {
            BlockCode(listOf("// Choose a Property for this Block."))
        }
    }

    /**
     * A type's own Property by its name, and a Manager type's through the field the generated class keeps it in
     * ([managersReachedBy] names that Manager type).
     */
    internal fun propertyExpression(reference: PropertyReference, scope: BlockScope) = scope.ownerOf(reference.manager).let { owner ->
        if (owner == "this") reference.name else "$owner.${reference.name}"
    }

    /**
     * The other Manager type a Property of [manager] is reached through, if it isn't the type's own.
     */
    private fun managersReachedBy(manager: String?, scope: BlockScope) = if (manager == null || scope.isOwn(manager)) emptySet() else setOf(manager)

    /**
     * A string literal for text whose placeholders show Properties.
     *
     * @param usesNumberText Whether it shows a number through [NUMBER_TEXT_FUNCTION].
     * @param managers The other Manager types whose Properties it shows.
     */
    internal data class TemplateLiteral(
        val code: String,
        val usesNumberText: Boolean,
        val managers: Set<String>,
    )

    /**
     * [text] as a string literal in which each placeholder ([PropertyPlaceholders]) is the value of its Property (a whole
     * number without its ".0", through [NUMBER_TEXT_FUNCTION]). Braces around anything else stay as they are.
     */
    internal fun templateLiteral(text: String, scope: BlockScope): TemplateLiteral {
        var end = 0
        var usesNumberText = false
        val managers = mutableSetOf<String>()
        val code = buildString {
            append('"')
            PropertyPlaceholders.find(text).forEach { placeholder ->
                val property = scope.propertyOf(placeholder) ?: return@forEach
                appendEscaped(text.substring(end, placeholder.range.first))
                append("\${").append(propertyExpression(PropertyReference(placeholder.name, PropertyKind.of(property), placeholder.manager), scope))
                if (property is PropertyDefinition.Number) {
                    append(".$NUMBER_TEXT_FUNCTION()")
                    usesNumberText = true
                }
                append('}')
                managers += managersReachedBy(placeholder.manager, scope)
                end = placeholder.range.last + 1
            }
            appendEscaped(text.substring(end))
            append('"')
        }
        return TemplateLiteral(code, usesNumberText, managers)
    }

    public fun stringLiteral(text: String): String = buildString {
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

    private fun offsetCode(x: NumberValue, y: NumberValue, scope: BlockScope) = "SceneOffset(${NumberValueCode.sceneUnit(x, scope)}, ${NumberValueCode.sceneUnit(y, scope)})"

    internal fun unitLiteral(value: Float) = "${floatLiteral(value)}.sceneUnit"

    internal fun floatLiteral(value: Float) = when {
        !value.isFinite() -> "0f"
        value == value.toLong().toFloat() && value.absoluteValue < 1e9f -> "${value.toLong()}f"
        else -> "${value}f"
    }

    internal fun colorLiteral(color: Long) = "0x" + (color and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
}
