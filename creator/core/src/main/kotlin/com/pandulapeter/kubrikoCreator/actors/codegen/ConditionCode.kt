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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionKind
import com.pandulapeter.kubrikoCreator.actors.ToggleValue
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.KEYBOARD_INPUT_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.KEY_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.STATE_MANAGER_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.managersReachedBy
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.propertyExpression
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.numberValues
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.actors.valuePropertyReferences

/**
 * The conditions if Blocks check and texts are shown while, as Kotlin expressions.
 */
internal object ConditionCode {

    /**
     * [condition] as a Kotlin expression (the only line of the result), with what its numbers reach, or `false` if it
     * needs a Property that doesn't exist (of that kind, a value it compares with read from one too), an Actor type that
     * doesn't, or an Actor in a Manager type, so that the generated code always compiles. A combined condition joins its conditions' expressions, each in parentheses.
     */
    fun of(condition: ConditionDefinition, scope: BlockScope): BlockCode = if (condition is ConditionDefinition.Combined) combinedOf(condition, scope) else expressionOf(condition, scope).let { code ->
        if (code.lines.single() == "false") {
            code
        } else {
            code.copy(needs = code.needs + NumberValueCode.needsOf(condition.numberValues), managers = code.managers + scope.managersReading(condition.valuePropertyReferences))
        }
    }

    private fun combinedOf(condition: ConditionDefinition.Combined, scope: BlockScope): BlockCode {
        if (condition.conditions.isEmpty()) return BlockCode(listOf(condition.isAll.toString()))
        val parts = condition.conditions.map { of(it, scope) }
        return parts.fold(BlockCode.NONE) { code, part -> code + part.copy(lines = emptyList()) }
            .copy(lines = listOf(parts.joinToString(if (condition.isAll) " && " else " || ") { "(${it.lines.single()})" }))
    }

    private fun expressionOf(condition: ConditionDefinition, scope: BlockScope): BlockCode {
        val reference = condition.propertyReference
        if (reference != null && !scope.hasProperty(reference)) return BlockCode(listOf("false"))
        // A value read from a Property that doesn't exist would otherwise be compared as 0, an empty text, or off.
        if (condition.valuePropertyReferences.any { !scope.hasProperty(it) }) return BlockCode(listOf("false"))
        if (!ConditionKind.of(condition).isAvailableIn(scope.kind)) return BlockCode(listOf("false"))
        val property = reference?.let { propertyExpression(it, scope) }
        val managers = managersReachedBy(reference?.manager, scope)
        return when (condition) {
            is ConditionDefinition.Number -> BlockCode(listOf("$property ${condition.comparison.operator} ${NumberValueCode.float(condition.value, scope)}"), managers = managers)
            is ConditionDefinition.Text -> BlockCode(listOf("$property ${if (condition.isEqual) "==" else "!="} ${TextAndToggleValueCode.text(condition.value, scope)}"), managers = managers)
            is ConditionDefinition.Toggle -> BlockCode(
                lines = listOf(
                    when (val isOn = condition.isOn) {
                        is ToggleValue.Fixed -> if (isOn.isOn) "$property" else "!$property"
                        is ToggleValue.Property -> "$property == ${TextAndToggleValueCode.toggle(isOn, scope)}"
                    },
                ),
                managers = managers,
            )
            is ConditionDefinition.KeyHeld -> if (condition.key.isKnown) {
                BlockCode(
                    lines = listOf((if (condition.isHeld) "" else "!") + "$KEYBOARD_INPUT_MANAGER_FIELD.isKeyPressed(Key.${condition.key.composeKeyName})"),
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
            is ConditionDefinition.Distance -> when (val type = condition.actorType) {
                is ActorTypeValue.Fixed -> type.name.takeIf { it in scope.actorTypeNames }?.let(::stringLiteral)
                is ActorTypeValue.Property -> ActorTypeValueCode.nameExpression(type, scope)
            }?.let { name ->
                BlockCode(
                    lines = listOf("${ActorTypeValueCode.distanceCall("actorManager.allActors.value", name, "this")} ${condition.comparison.operator} ${NumberValueCode.float(condition.value, scope)}"),
                    needs = setOf(BlockNeed.ACTOR_MANAGER),
                )
            } ?: BlockCode(listOf("false"))
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
            is ConditionDefinition.Combined -> combinedOf(condition, scope)
        }
    }
}
