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

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberValueFieldsTest {

    @Test
    fun readingAndReplacingABlocksNumbersFindTheSameFields() {
        val blocks = BlockKind.entries.map { it.createDefault() } + EveryBlockTypes.everyBlock +
            ConditionKind.entries.map { BlockDefinition.If(it.createDefault()) }
        blocks.forEach { block ->
            val replaced = mutableListOf<NumberValue>()
            val marked = block.withNumberValues { value ->
                replaced += value
                NumberValue.Fixed(replaced.size.toFloat())
            }
            assertEquals(replaced, block.numberValues, block.toString())
            assertEquals(replaced.indices.map { NumberValue.Fixed(it + 1f) }, marked.numberValues, block.toString())
        }
    }
}
