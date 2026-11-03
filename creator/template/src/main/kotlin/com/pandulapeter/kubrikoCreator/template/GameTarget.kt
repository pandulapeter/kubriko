/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A platform a game can be built for, each a target of its Kotlin Multiplatform build.
 *
 * @param key How the AI names it.
 */
@Serializable
public enum class GameTarget(public val key: String) {
    @SerialName("desktop")
    DESKTOP("desktop"),

    @SerialName("web")
    WEB("web"),

    @SerialName("android")
    ANDROID("android"),

    @SerialName("ios")
    IOS("ios"),
}
