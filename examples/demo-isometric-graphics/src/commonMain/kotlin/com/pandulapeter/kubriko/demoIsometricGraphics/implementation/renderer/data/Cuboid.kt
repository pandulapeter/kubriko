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

import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableAngleRadians
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableColor
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: Use Vec3
@Serializable
data class Cuboid(
    @SerialName("name") var name: String = "",
    @SerialName("positionX") var positionX: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("positionY") var positionY: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("positionZ") var positionZ: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("sizeX") var sizeX: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("sizeY") var sizeY: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("sizeZ") var sizeZ: SerializableSceneUnit = SceneUnit.Zero,
    @SerialName("rotationX") var rotationX: SerializableAngleRadians = AngleRadians.Zero,
    @SerialName("rotationY") var rotationY: SerializableAngleRadians = AngleRadians.Zero,
    @SerialName("rotationZ") var rotationZ: SerializableAngleRadians = AngleRadians.Zero,
    @SerialName("colorXPlus") var colorXPlus: SerializableColor? = null,
    @SerialName("textureXPlus") var textureXPlus: String? = null,
    @SerialName("colorXMinus") var colorXMinus: SerializableColor? = null,
    @SerialName("textureXMinus") var textureXMinus: String? = null,
    @SerialName("colorYPlus") var colorYPlus: SerializableColor? = null,
    @SerialName("textureYPlus") var textureYPlus: String? = null,
    @SerialName("colorYMinus") var colorYMinus: SerializableColor? = null,
    @SerialName("textureYMinus") var textureYMinus: String? = null,
    @SerialName("colorZPlus") var colorZPlus: SerializableColor? = null,
    @SerialName("textureZPlus") var textureZPlus: String? = null,
    @SerialName("colorZMinus") var colorZMinus: SerializableColor? = null,
    @SerialName("textureZMinus") var textureZMinus: String? = null,
)