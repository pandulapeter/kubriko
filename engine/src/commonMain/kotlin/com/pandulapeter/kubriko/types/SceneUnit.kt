/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.types

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import kotlin.jvm.JvmInline

/**
 * A wrapper for a single dimension in the coordinate system of the scene.
 *
 * Regular [Float] values should be used for dimensions in screen pixels, while [SceneUnit]
 * should be used for logical dimensions within the game world.
 *
 * Use the `sceneUnit` extension property on [Float] to create an instance.
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