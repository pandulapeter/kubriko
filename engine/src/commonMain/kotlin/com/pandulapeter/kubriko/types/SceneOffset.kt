package com.pandulapeter.kubriko.types

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import kotlin.jvm.JvmInline

/**
 * Defines an [Offset] in [ScenePixel]-s.
 *
 * Regular [Offset] values should be used for coordinates on the screen while this wrapper should be used for coordinates within the Scene.
 */
@JvmInline
value class SceneOffset(val raw: Offset) {
    val x: ScenePixel get() = raw.x.scenePixel
    val y: ScenePixel get() = raw.y.scenePixel

    constructor(x: ScenePixel, y: ScenePixel) : this(Offset(x.raw, y.raw))

    operator fun plus(other: SceneOffset): SceneOffset = SceneOffset(raw + other.raw)

    operator fun minus(other: SceneOffset): SceneOffset = SceneOffset(raw - other.raw)

    operator fun times(scale: Float): SceneOffset = SceneOffset(raw * scale)

    operator fun times(scale: Int): SceneOffset = SceneOffset(raw * scale.toFloat())

    operator fun times(scale: ScenePixel): SceneOffset = SceneOffset(raw * scale.raw)

    operator fun times(scale: Scale): SceneOffset = SceneOffset(x * scale.horizontal, y * scale.vertical)

    operator fun div(scale: Float): SceneOffset = SceneOffset(raw / scale)

    operator fun div(scale: Int): SceneOffset = SceneOffset(raw / scale.toFloat())

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
        val Zero = SceneOffset(ScenePixel.Zero, ScenePixel.Zero)
        val Left = SceneOffset(-ScenePixel.Unit, ScenePixel.Zero)
        val TopLeft = SceneOffset(-ScenePixel.Unit, -ScenePixel.Unit)
        val Top = SceneOffset(ScenePixel.Zero, -ScenePixel.Unit)
        val TopRight = SceneOffset(ScenePixel.Unit, -ScenePixel.Unit)
        val Right = SceneOffset(ScenePixel.Unit, ScenePixel.Zero)
        val BottomRight = SceneOffset(ScenePixel.Unit, ScenePixel.Unit)
        val Bottom = SceneOffset(ScenePixel.Zero, ScenePixel.Unit)
        val BottomLeft = SceneOffset(-ScenePixel.Unit, ScenePixel.Unit)
    }
}