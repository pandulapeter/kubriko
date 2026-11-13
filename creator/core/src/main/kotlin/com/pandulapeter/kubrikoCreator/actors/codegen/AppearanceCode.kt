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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.OPACITY_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.colorLiteral

/**
 * The statements of the Blocks that change how an Actor looks, through its `color` and its `opacity`, which its look reads
 * every frame. The opacity is a `Float` of its own rather than the color's alpha, which Compose keeps in 1/255 steps, so a
 * fade by small steps adds up; and a color keeps the alpha it had, so a translucent type stays as translucent. Every
 * value is a `Color`, an inline class, or a `Float`, so none of them allocates.
 */
internal object AppearanceCode {
    fun setColor(block: BlockDefinition.SetColor): BlockCode {
        val colors = block.colors.map { "Color(${colorLiteral(it)})" }
        val color = when (colors.size) {
            0 -> return BlockCode(listOf("// Choose a color for this Block."))
            1 -> colors.single()
            else -> colors.dropLast(1).withIndex().joinToString(separator = "; ", prefix = "when (${NumberValueCode.RANDOM}.nextInt(${colors.size})) { ", postfix = "; else -> ${colors.last()} }") { (index, color) -> "$index -> $color" }
        }
        return BlockCode(listOf("color = $color.copy(alpha = color.alpha)"))
    }

    fun setOpacity(block: BlockDefinition.SetOpacity, scope: BlockScope) = BlockCode(
        listOf("$OPACITY_FIELD = (${NumberValueCode.float(block.percent, scope)} / 100f).coerceIn(0f, 1f)"),
    )

    fun changeOpacity(block: BlockDefinition.ChangeOpacity, scope: BlockScope) = BlockCode(
        listOf("$OPACITY_FIELD = ($OPACITY_FIELD + ${NumberValueCode.float(block.by, scope)} / 100f).coerceIn(0f, 1f)"),
    )
}
