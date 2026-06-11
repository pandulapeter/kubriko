/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data

import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CuboidKeyframe(
    @SerialName("positionX") val positionX: Float? = null,
    @SerialName("positionY") val positionY: Float? = null,
    @SerialName("positionZ") val positionZ: Float? = null,
    @SerialName("sizeX") val sizeX: Float? = null,
    @SerialName("sizeY") val sizeY: Float? = null,
    @SerialName("sizeZ") val sizeZ: Float? = null,
    @SerialName("rotationX") val rotationX: Float? = null,
    @SerialName("rotationY") val rotationY: Float? = null,
    @SerialName("rotationZ") val rotationZ: Float? = null,
    @SerialName("colorXPlus") val colorXPlus: KeyframedColor? = null,
    @SerialName("colorXMinus") val colorXMinus: KeyframedColor? = null,
    @SerialName("colorYPlus") val colorYPlus: KeyframedColor? = null,
    @SerialName("colorYMinus") val colorYMinus: KeyframedColor? = null,
    @SerialName("colorZPlus") val colorZPlus: KeyframedColor? = null,
    @SerialName("colorZMinus") val colorZMinus: KeyframedColor? = null,
    // Interpolation type for the interval ending at this keyframe (true = cubic Catmull-Rom, false = linear)
    @SerialName("positionXCubic") val positionXCubic: Boolean = true,
    @SerialName("positionYCubic") val positionYCubic: Boolean = true,
    @SerialName("positionZCubic") val positionZCubic: Boolean = true,
    @SerialName("sizeXCubic") val sizeXCubic: Boolean = true,
    @SerialName("sizeYCubic") val sizeYCubic: Boolean = true,
    @SerialName("sizeZCubic") val sizeZCubic: Boolean = true,
    @SerialName("rotationXCubic") val rotationXCubic: Boolean = true,
    @SerialName("rotationYCubic") val rotationYCubic: Boolean = true,
    @SerialName("rotationZCubic") val rotationZCubic: Boolean = true,
    @SerialName("colorXPlusCubic") val colorXPlusCubic: Boolean = true,
    @SerialName("colorXMinusCubic") val colorXMinusCubic: Boolean = true,
    @SerialName("colorYPlusCubic") val colorYPlusCubic: Boolean = true,
    @SerialName("colorYMinusCubic") val colorYMinusCubic: Boolean = true,
    @SerialName("colorZPlusCubic") val colorZPlusCubic: Boolean = true,
    @SerialName("colorZMinusCubic") val colorZMinusCubic: Boolean = true,
) {
    val isEmpty: Boolean
        get() = positionX == null && positionY == null && positionZ == null
                && sizeX == null && sizeY == null && sizeZ == null
                && rotationX == null && rotationY == null && rotationZ == null
                && colorXPlus == null && colorXMinus == null
                && colorYPlus == null && colorYMinus == null
                && colorZPlus == null && colorZMinus == null
}

// Wraps a nullable color so the outer null means "not keyed" and the inner null means "keyed as no color (use texture)".
@Serializable
data class KeyframedColor(
    @SerialName("color") val color: SerializableColor? = null,
)
