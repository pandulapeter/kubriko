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

/**
 * Where a Block stands in an Event's flat list of Blocks, in which if, else, repeat, and end Blocks mark sections, as
 * GameMaker's actions do.
 *
 * @param depth How many sections it is in. An else or end Block is at the depth of the Block that opened its section.
 */
internal data class BlockNesting(
    val depth: Int,
    val role: Role,
) {
    enum class Role {
        PLAIN,

        /**
         * An if or repeat Block, whose section runs up to its end Block, or to the end of the Event without one.
         */
        OPENS,
        ELSE,
        CLOSES,

        /**
         * An else Block outside an if Block's section (or a second one in it), or an end Block with nothing to end,
         * which does nothing.
         */
        STRAY,
    }

    companion object {

        /**
         * The nesting of each of [blocks], and how many sections are still open after the last one.
         */
        fun of(blocks: List<BlockDefinition>): Pair<List<BlockNesting>, Int> {
            val openSections = ArrayDeque<Boolean>()
            val nesting = blocks.map { block ->
                when (block) {
                    is BlockDefinition.If, is BlockDefinition.Repeat -> BlockNesting(openSections.size, Role.OPENS).also { openSections.addLast(block is BlockDefinition.If) }
                    BlockDefinition.Else -> if (openSections.lastOrNull() == true) {
                        openSections[openSections.lastIndex] = false
                        BlockNesting(openSections.size - 1, Role.ELSE)
                    } else {
                        BlockNesting(openSections.size, Role.STRAY)
                    }
                    BlockDefinition.End -> if (openSections.isNotEmpty()) {
                        openSections.removeLast()
                        BlockNesting(openSections.size, Role.CLOSES)
                    } else {
                        BlockNesting(0, Role.STRAY)
                    }
                    else -> BlockNesting(openSections.size, Role.PLAIN)
                }
            }
            return nesting to openSections.size
        }
    }
}
