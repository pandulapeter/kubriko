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

import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The Kotlin expressions of the numbers Blocks take ([NumberValue]). Each is one operand that needs no parentheses around
 * it, so it can stand before `.sceneUnit` or beside an operator, and none allocates. Random numbers all come from the one
 * [ManagerTypeCodeGenerator.RANDOM_FIELD] of the wiring, which a test can seed.
 */
internal object NumberValueCode {

    private const val RANDOM = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.RANDOM_FIELD}"

    /**
     * [value] as a `Float`. A Property that doesn't exist (renamed, deleted, or not chosen yet) reads as 0, so that the
     * generated code always compiles.
     */
    fun float(value: NumberValue, scope: BlockScope): String = when (value) {
        is NumberValue.Fixed -> BlockCodeGenerator.floatLiteral(value.value)
        is NumberValue.Random -> {
            val from = min(value.from, value.to)
            val to = max(value.from, value.to)
            when {
                value.isWhole -> wholeRandom(from, to)?.let { "$it.toFloat()" } ?: BlockCodeGenerator.floatLiteral(from.roundToInt().toFloat())
                from == to -> BlockCodeGenerator.floatLiteral(from)
                else -> "($RANDOM.nextFloat() * ${BlockCodeGenerator.floatLiteral(to - from)} ${if (from < 0f) "-" else "+"} ${BlockCodeGenerator.floatLiteral(abs(from))})"
            }
        }
        is NumberValue.Property -> propertyOf(value, scope) ?: "0f"
    }

    /**
     * [value] as an `Int` of at least [minimum]: a fraction is dropped, and a random number is drawn from the whole numbers
     * between its two, both included.
     */
    fun whole(value: NumberValue, scope: BlockScope, minimum: Int = 0): String = when (value) {
        is NumberValue.Fixed -> value.value.toInt().coerceAtLeast(minimum).toString()
        is NumberValue.Random -> {
            val from = max(min(value.from, value.to), minimum.toFloat())
            val to = max(value.from, value.to)
            wholeRandom(from, to) ?: from.roundToInt().coerceAtLeast(minimum).toString()
        }
        is NumberValue.Property -> propertyOf(value, scope)?.let { "$it.toInt().coerceAtLeast($minimum)" } ?: minimum.toString()
    }

    /**
     * [value] in scene units.
     */
    fun sceneUnit(value: NumberValue, scope: BlockScope) = float(value, scope) + ".sceneUnit"

    /**
     * The other Manager types whose Properties [values] read, which the generated class has to look up.
     */
    fun managersReadBy(values: List<NumberValue>, scope: BlockScope) = values.mapNotNullTo(HashSet()) { value ->
        value.propertyReference?.takeIf { scope.hasProperty(it) }?.manager?.takeUnless(scope::isOwn)
    }

    /**
     * A random whole number from [from] to [to], both included, or null if there is at most one between them.
     */
    private fun wholeRandom(from: Float, to: Float): String? {
        val first = ceil(from).toInt()
        val last = floor(to).toInt()
        return if (last > first) "$RANDOM.nextInt($first, ${last + 1})" else null
    }

    private fun propertyOf(value: NumberValue.Property, scope: BlockScope) =
        value.propertyReference?.takeIf { scope.hasProperty(it) }?.let { BlockCodeGenerator.propertyExpression(it, scope) }
}
