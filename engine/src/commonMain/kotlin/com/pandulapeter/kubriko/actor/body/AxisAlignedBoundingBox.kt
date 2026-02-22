/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.body

import androidx.compose.ui.util.fastRoundToInt
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.jvm.JvmInline

@JvmInline
value class AxisAlignedBoundingBox(private val packed: Long) {

    constructor(min: SceneOffset, max: SceneOffset) : this(
        (((quantizeMin(min.x.raw.fastRoundToInt()) and 0xFFFF).toLong()) shl 48) or
                (((quantizeMin(min.y.raw.fastRoundToInt()) and 0xFFFF).toLong()) shl 32) or
                (((quantizeMax(max.x.raw.fastRoundToInt()) and 0xFFFF).toLong()) shl 16) or
                (((quantizeMax(max.y.raw.fastRoundToInt()) and 0xFFFF).toLong()))
    )

    private val minXQ: Int get() = (packed shr 48).toShort().toInt()
    private val minYQ: Int get() = (packed shr 32).toShort().toInt()
    private val maxXQ: Int get() = (packed shr 16).toShort().toInt()
    private val maxYQ: Int get() = packed.toShort().toInt()

    val minXRaw: Int get() = minXQ shl QUANT_SHIFT
    val minYRaw: Int get() = minYQ shl QUANT_SHIFT
    val maxXRaw: Int get() = maxXQ shl QUANT_SHIFT
    val maxYRaw: Int get() = maxYQ shl QUANT_SHIFT

    val left get() = minXRaw.sceneUnit
    val top get() = minYRaw.sceneUnit
    val right get() = maxXRaw.sceneUnit
    val bottom get() = maxYRaw.sceneUnit
    val min: SceneOffset get() = SceneOffset(left, top)
    val max: SceneOffset get() = SceneOffset(right, bottom)
    val size
        get() = SceneSize(
            width = right - left,
            height = bottom - top,
        )

    companion object {
        const val QUANT_SHIFT: Int = 4 // Lower value results in more precision but limits the size of the scene
        private const val STEP: Int = 1 shl QUANT_SHIFT
        private const val MASK: Int = STEP - 1

        private fun clampToShort(i: Int) = i.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

        private fun floorToStep(raw: Int) = raw and MASK.inv()

        private fun ceilToStep(raw: Int): Int {
            val floored = floorToStep(raw)
            return if (raw == floored) raw else floored + STEP
        }

        private fun quantizeMin(raw: Int): Int = clampToShort(floorToStep(raw) shr QUANT_SHIFT)

        private fun quantizeMax(raw: Int): Int = clampToShort(ceilToStep(raw) shr QUANT_SHIFT)
    }
}