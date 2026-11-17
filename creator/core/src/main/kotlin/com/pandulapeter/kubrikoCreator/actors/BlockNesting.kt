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
public data class BlockNesting(
    val depth: Int,
    val role: Role,
) {
    /**
     * The part a Block plays in the sections it opens and closes.
     */
    public enum class Role {
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

    public companion object {

        /**
         * The nesting of each of [blocks], and how many sections are still open after the last one.
         */
        public fun of(blocks: List<BlockDefinition>): Pair<List<BlockNesting>, Int> {
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

        /**
         * The indexes of the if or repeat Block at [index] of [blocks] and of every Block of its section: up to its end
         * Block, or to the end of the Event without one.
         */
        public fun sectionOf(blocks: List<BlockDefinition>, index: Int): IntRange = index..(endOf(blocks, index) ?: blocks.lastIndex)

        /**
         * The index of the end Block of the if or repeat Block at [index] of [blocks], or null if its section runs to the
         * end of the Event.
         */
        public fun endOf(blocks: List<BlockDefinition>, index: Int): Int? {
            val nesting = of(blocks).first
            val depth = nesting[index].depth
            return (index + 1 until blocks.size).firstOrNull { nesting[it].depth == depth && nesting[it].role == Role.CLOSES }
        }

        /**
         * The Blocks that go into another Event with the one at [index] of [blocks]: an if or repeat Block takes its
         * section, any other Block goes alone, and an else or end Block goes nowhere (null), since in another Event it
         * would end or split a section it doesn't belong to.
         */
        public fun movingBetweenEvents(blocks: List<BlockDefinition>, index: Int): MovingBlocks? = when (blocks[index]) {
            is BlockDefinition.If, is BlockDefinition.Repeat -> endOf(blocks, index).let { end -> MovingBlocks(index..(end ?: blocks.lastIndex), isUnendedSection = end == null) }
            BlockDefinition.Else, BlockDefinition.End -> null
            else -> MovingBlocks(index..index, isUnendedSection = false)
        }
    }
}

/**
 * The Blocks at [indexes] of an Event that go into another one together ([BlockNesting.movingBetweenEvents]).
 *
 * @param isUnendedSection Whether they are an if or repeat Block's section without an end Block, which runs to the end of
 *   whichever Event it is in, so it goes only after another Event's last Block.
 */
public data class MovingBlocks(
    val indexes: IntRange,
    val isUnendedSection: Boolean,
)
