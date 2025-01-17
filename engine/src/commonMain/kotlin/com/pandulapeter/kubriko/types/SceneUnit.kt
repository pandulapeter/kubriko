/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import com.pandulapeter.kubriko.extensions.sceneUnit
import kotlin.jvm.JvmInline

/**
 * Wrapper for 1D dimensions in the context of a Scene.
 * Regular [Float] values should be used for screen dimensions while this wrapper should be used for Scene dimensions.
 *
 * Use the [sceneUnit] extension property for casting.
 */
@JvmInline
value class SceneUnit internal constructor(val raw: Float) : Comparable<SceneUnit> {

    operator fun plus(other: SceneUnit): SceneUnit = (raw + other.raw).sceneUnit

    operator fun unaryPlus(): SceneUnit = raw.unaryPlus().sceneUnit

    operator fun minus(other: SceneUnit): SceneUnit = (raw - other.raw).sceneUnit

    operator fun unaryMinus(): SceneUnit = raw.unaryMinus().sceneUnit

    operator fun times(scale: Float): SceneUnit = (raw * scale).sceneUnit

    operator fun times(sceneUnit: SceneUnit): SceneUnit = (raw * sceneUnit.raw).sceneUnit

    operator fun times(scale: Int): SceneUnit = (raw * scale.toFloat()).sceneUnit

    operator fun div(scale: Float): SceneUnit = (raw / scale).sceneUnit

    operator fun div(scale: SceneUnit): SceneUnit = (raw / scale.raw).sceneUnit

    operator fun div(scale: Int): SceneUnit = (raw / scale.toFloat()).sceneUnit

    override operator fun compareTo(other: SceneUnit) = raw.compareTo(other.raw)

    operator fun rangeTo(other: SceneUnit) = raw.rangeTo(other.raw)

    operator fun rangeUntil(other: SceneUnit) = raw.rangeUntil(other.raw)

    override fun toString(): String = "SceneUnit($raw)"

    companion object {
        val Zero = SceneUnit(0f)
        val Unit = SceneUnit(1f)
    }
}