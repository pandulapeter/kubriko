package com.pandulapeter.kubriko.types

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.extensions.sceneUnit
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
        val TopLeft = SceneOffset(-SceneUnit.Unit, -SceneUnit.Unit)
        val Top = SceneOffset(SceneUnit.Zero, -SceneUnit.Unit)
        val TopRight = SceneOffset(SceneUnit.Unit, -SceneUnit.Unit)
        val Right = SceneOffset(SceneUnit.Unit, SceneUnit.Zero)
        val BottomRight = SceneOffset(SceneUnit.Unit, SceneUnit.Unit)
        val Bottom = SceneOffset(SceneUnit.Zero, SceneUnit.Unit)
        val BottomLeft = SceneOffset(-SceneUnit.Unit, SceneUnit.Unit)
    }
}