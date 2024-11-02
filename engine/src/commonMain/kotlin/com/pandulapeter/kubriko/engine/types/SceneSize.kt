package com.pandulapeter.kubriko.engine.types

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import com.pandulapeter.kubriko.engine.implementation.extensions.scenePixel
import kotlin.jvm.JvmInline

/**
 * Defines a [Size] in [ScenePixel]-s.
 *
 * Regular [Size] values should be used for screen sizes while this wrapper should be used for Scene sizes.
 */
@JvmInline
value class SceneSize(val raw: Size) {
    val width: ScenePixel get() = raw.width.scenePixel
    val height: ScenePixel get() = raw.height.scenePixel
    val center get() = SceneOffset(raw.center)

    constructor(width: ScenePixel, height: ScenePixel) : this(Size(width.raw, height.raw))

    operator fun plus(other: SceneSize): SceneSize = SceneSize(
        width = width + other.width,
        height = height + other.height,
    )

    operator fun minus(other: SceneSize): SceneSize = SceneSize(
        width = width - other.width,
        height = height - other.height,
    )

    operator fun times(scale: Float): SceneSize = SceneSize(raw * scale)

    operator fun times(scale: Int): SceneSize = SceneSize(raw * scale.toFloat())

    operator fun times(scale: ScenePixel): SceneSize = SceneSize(raw * scale.raw)

    operator fun div(scale: Float): SceneSize = SceneSize(raw / scale)

    operator fun div(scale: Int): SceneSize = SceneSize(raw / scale.toFloat())

    override fun toString(): String = "SceneSize(width=$width, height=$height)"

    companion object {
        val Zero = SceneSize(Size.Zero)
    }
}