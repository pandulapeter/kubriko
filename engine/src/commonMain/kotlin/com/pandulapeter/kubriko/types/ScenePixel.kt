package com.pandulapeter.kubriko.types

import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import kotlin.jvm.JvmInline

/**
 * Wrapper for 1D dimensions in the context of a Scene.
 * Regular [Float] values should be used for screen dimensions while this wrapper should be used for Scene dimensions.
 *
 * Use the [scenePixel] extension property for casting.
 */
@JvmInline
value class ScenePixel internal constructor(val raw: Float) : Comparable<ScenePixel> {

    operator fun plus(other: ScenePixel): ScenePixel = (raw + other.raw).scenePixel

    operator fun unaryPlus(): ScenePixel = raw.unaryPlus().scenePixel

    operator fun minus(other: ScenePixel): ScenePixel = (raw - other.raw).scenePixel

    operator fun unaryMinus(): ScenePixel = raw.unaryMinus().scenePixel

    operator fun times(scale: Float): ScenePixel = (raw * scale).scenePixel

    operator fun times(scale: Int): ScenePixel = (raw * scale.toFloat()).scenePixel

    operator fun div(scale: Float): ScenePixel = (raw / scale).scenePixel

    operator fun div(scale: Int): ScenePixel = (raw / scale.toFloat()).scenePixel

    override operator fun compareTo(other: ScenePixel) = raw.compareTo(other.raw)

    operator fun rangeTo(other: ScenePixel) = raw.rangeTo(other.raw)

    operator fun rangeUntil(other: ScenePixel) = raw.rangeUntil(other.raw)

    override fun toString(): String = "ScenePixel($raw)"

    companion object {
        val Zero = ScenePixel(0f)
    }
}