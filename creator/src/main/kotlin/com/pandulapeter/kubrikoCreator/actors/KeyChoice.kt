/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The keys a Key pressed Event can react to.
 *
 * @param composeKeyName The name of the matching `androidx.compose.ui.input.key.Key` constant.
 */
@Serializable
internal enum class KeyChoice(
    val composeKeyName: String,
) {
    @SerialName("left")
    LEFT("DirectionLeft"),

    @SerialName("right")
    RIGHT("DirectionRight"),

    @SerialName("up")
    UP("DirectionUp"),

    @SerialName("down")
    DOWN("DirectionDown"),

    @SerialName("space")
    SPACE("Spacebar"),

    @SerialName("enter")
    ENTER("Enter"),

    @SerialName("w")
    W("W"),

    @SerialName("a")
    A("A"),

    @SerialName("s")
    S("S"),

    @SerialName("d")
    D("D"),
}
