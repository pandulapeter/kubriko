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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CuboidAnimation(
    @SerialName("name") var name: String = "",
    @SerialName("length") var length: Int = 0,
    // Outer key: cuboid ID; inner key: timestamp in ms; value: which properties are explicitly set at that timestamp.
    @SerialName("keyframes") val keyframes: LinkedHashMap<String, LinkedHashMap<Int, CuboidKeyframe>> = linkedMapOf(),
)