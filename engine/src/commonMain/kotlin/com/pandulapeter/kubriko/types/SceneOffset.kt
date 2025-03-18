/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import kotlin.jvm.JvmInline

/**
 * Defines an [Offset] in [SceneUnit]-s.
 *
 * Regular [Offset] values should be used for coordinates on the screen while this wrapper should be used for coordinates within the Scene.
 */
@JvmInline
value class SceneOffset(val raw: Offset) {
    val x: SceneUnit get() = raw.x.sceneUnit
    val y: SceneUnit get() = raw.y.sceneUnit

    constructor(x: SceneUnit, y: SceneUnit) : this(Offset(x.raw, y.raw))

    constructor(direction: AngleRadians) : this(direction.cos.sceneUnit, direction.sin.sceneUnit)

    fun copy(x: SceneUnit = this.x, y: SceneUnit = this.y) = SceneOffset(
        x = x,
        y = y,
    )

    operator fun plus(other: SceneOffset): SceneOffset = SceneOffset(raw + other.raw)

    operator fun minus(other: SceneOffset): SceneOffset = SceneOffset(raw - other.raw)

    operator fun times(scale: Float): SceneOffset = SceneOffset(raw * scale)

    operator fun times(scale: Int): SceneOffset = SceneOffset(raw * scale.toFloat())

    operator fun times(scale: SceneUnit): SceneOffset = SceneOffset(raw * scale.raw)

    operator fun times(scale: Scale): SceneOffset = SceneOffset(
        x = x * scale.horizontal,
        y = y * scale.vertical,
    )

    operator fun div(scale: Float): SceneOffset = SceneOffset(raw / scale)

    operator fun div(scale: Int): SceneOffset = SceneOffset(raw / scale.toFloat())

    operator fun div(scale: Scale): SceneOffset = SceneOffset(
        x = x / scale.horizontal,
        y = y / scale.vertical,
    )

    operator fun unaryMinus(): SceneOffset = SceneOffset(
        x = x.unaryMinus(),
        y = y.unaryMinus(),
    )

    operator fun unaryPlus(): SceneOffset = SceneOffset(
        x = x.unaryPlus(),
        y = y.unaryPlus(),
    )

    override fun toString(): String = "SceneOffset(x=$x, y=$y)"

    companion object {
        val Zero = SceneOffset(SceneUnit.Zero, SceneUnit.Zero)
        val Left = SceneOffset(-SceneUnit.Unit, SceneUnit.Zero)
        val UpLeft = SceneOffset(-SceneUnit.Unit, -SceneUnit.Unit)
        val Up = SceneOffset(SceneUnit.Zero, -SceneUnit.Unit)
        val UpRight = SceneOffset(SceneUnit.Unit, -SceneUnit.Unit)
        val Right = SceneOffset(SceneUnit.Unit, SceneUnit.Zero)
        val DownRight = SceneOffset(SceneUnit.Unit, SceneUnit.Unit)
        val Down = SceneOffset(SceneUnit.Zero, SceneUnit.Unit)
        val DownLeft = SceneOffset(-SceneUnit.Unit, SceneUnit.Unit)
    }
}