/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
    val min: SceneOffset
        get() = SceneOffset(
            x = (packed shr 48).toShort().toInt().sceneUnit,
            y = (packed shr 32).toShort().toInt().sceneUnit,
        )

    val max: SceneOffset
        get() = SceneOffset(
            x = (packed shr 16).toShort().toInt().sceneUnit,
            y = packed.toShort().toInt().sceneUnit,
        )

    constructor(min: SceneOffset, max: SceneOffset) : this(
        (((min.x.raw.fastRoundToInt()) and 0xFFFF).toLong() shl 48) or
                (((min.y.raw.fastRoundToInt()) and 0xFFFF).toLong() shl 32) or
                (((max.x.raw.fastRoundToInt()) and 0xFFFF).toLong() shl 16) or
                ((max.y.raw.fastRoundToInt()) and 0xFFFF).toLong()
    )

    val size
        get() = SceneSize(
            width = right - left,
            height = bottom - top,
        )

    val left get() = min.x
    val top get() = min.y
    val right get() = max.x
    val bottom get() = max.y
}