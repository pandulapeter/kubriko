/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.NumberValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What a number field of a Block holds, as its menu offers the choice.
 */
internal enum class NumberValueKind {
    FIXED,
    RANDOM,
    RANDOM_WHOLE,
    PROPERTY,
    ;

    /**
     * [value] turned into this kind, keeping what it can: a fixed number becomes a random one between 0 and it, and a
     * random one becomes the number it starts from, rounded in a field that only takes whole numbers.
     */
    fun of(value: NumberValue, isWholeField: Boolean = false): NumberValue = when (this) {
        FIXED -> when (value) {
            is NumberValue.Fixed -> value
            is NumberValue.Random -> NumberValue.Fixed(if (isWholeField) value.from.roundToInt().toFloat() else value.from)
            is NumberValue.Property -> NumberValue.Fixed(0f)
        }
        RANDOM, RANDOM_WHOLE -> when (value) {
            is NumberValue.Fixed -> if (value.value == 0f) {
                NumberValue.Random(from = 0f, to = 1f, isWhole = this == RANDOM_WHOLE)
            } else {
                NumberValue.Random(from = min(0f, value.value), to = max(0f, value.value), isWhole = this == RANDOM_WHOLE)
            }
            is NumberValue.Random -> value.copy(isWhole = this == RANDOM_WHOLE)
            is NumberValue.Property -> NumberValue.Random(isWhole = this == RANDOM_WHOLE)
        }
        PROPERTY -> value as? NumberValue.Property ?: NumberValue.Property()
    }

    companion object {

        /**
         * The kinds a field offers: one that only takes whole numbers draws whole numbers anyway, so it offers one random
         * kind.
         */
        fun offeredBy(isWholeField: Boolean) = if (isWholeField) listOf(FIXED, RANDOM, PROPERTY) else entries

        fun of(value: NumberValue, isWholeField: Boolean) = when (value) {
            is NumberValue.Fixed -> FIXED
            is NumberValue.Random -> if (value.isWhole && !isWholeField) RANDOM_WHOLE else RANDOM
            is NumberValue.Property -> PROPERTY
        }
    }
}
