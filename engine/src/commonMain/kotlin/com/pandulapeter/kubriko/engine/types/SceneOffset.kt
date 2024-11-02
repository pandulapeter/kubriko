package com.pandulapeter.kubriko.engine.types

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.engine.implementation.extensions.scenePixel
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

    operator fun div(scale: Float): SceneOffset = SceneOffset(raw / scale)

    operator fun div(scale: Int): SceneOffset = SceneOffset(raw / scale.toFloat())

    override fun toString(): String = "SceneOffset(x=$x, y=$y)"

    companion object {
        val Zero = SceneOffset(Offset.Zero)
    }
}