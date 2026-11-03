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

/**
 * The statements of Blocks, and what the class they are generated into has to declare and import for them.
 *
 * @param imports The fully qualified names the [lines] use directly.
 * @param managers The other Manager types whose fields the [lines] use.
 */
internal data class BlockCode(
    val lines: List<String>,
    val needs: Set<BlockNeed> = emptySet(),
    val imports: Set<String> = emptySet(),
    val managers: Set<String> = emptySet(),
) {
    operator fun plus(other: BlockCode) = BlockCode(
        lines = lines + other.lines,
        needs = needs + other.needs,
        imports = imports + other.imports,
        managers = managers + other.managers,
    )

    fun indented(depth: Int) = if (depth == 0) this else copy(lines = lines.map { BlockCodeGenerator.INDENT.repeat(depth) + it })

    companion object {

        val NONE = BlockCode(emptyList())
    }
}

/**
 * A member the class Blocks are generated into has to declare for them. How it is declared differs per kind of class, so
 * each generator declares them itself.
 */
internal enum class BlockNeed {
    /** The velocity field, and the `Dynamic` Trait that moves the Actor by it. */
    MOVEMENT,
    ACTOR_MANAGER,
    VIEWPORT_MANAGER,
    KEYBOARD_INPUT_MANAGER,
    STATE_MANAGER,

    /** The generated Manager that loads Scenes. */
    SCENES,

    /** The generated Manager that plays sounds and music. */
    AUDIO,

    /** The function that turns a number into text without its ".0". */
    NUMBER_TEXT,

    /** The function that bounces the Actor off another one. */
    BOUNCE_OFF,
}
