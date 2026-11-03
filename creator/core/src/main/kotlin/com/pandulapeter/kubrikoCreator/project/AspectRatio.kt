/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How the game's viewport fits the window, one of Kubriko's `ViewportManager.AspectRatioMode`s, by the name the
 * `game.aspectRatio` key of `gradle.properties` gives it.
 *
 * @param usesWidth Whether the mode reads [GameSettings.viewportWidth].
 * @param usesHeight Whether the mode reads [GameSettings.viewportHeight].
 */
@Serializable
public enum class AspectRatio(
    public val key: String,
    public val usesWidth: Boolean,
    public val usesHeight: Boolean,
) {
    /**
     * As many scene units as the window has pixels, so a larger window shows more of the Scene.
     */
    @SerialName("dynamic")
    DYNAMIC("dynamic", usesWidth = false, usesHeight = false),

    /**
     * The viewport's width always fits the window; how much of the Scene shows vertically follows the window's shape.
     */
    @SerialName("fitHorizontal")
    FIT_HORIZONTAL("fitHorizontal", usesWidth = true, usesHeight = false),

    /**
     * The viewport's height always fits the window; how much of the Scene shows horizontally follows the window's shape.
     */
    @SerialName("fitVertical")
    FIT_VERTICAL("fitVertical", usesWidth = false, usesHeight = true),

    /**
     * Always the viewport's width and height, with bars around it where the window's shape differs.
     */
    @SerialName("fixed")
    FIXED("fixed", usesWidth = true, usesHeight = true),

    /**
     * Always the viewport's width and height, stretched to the window's shape.
     */
    @SerialName("stretched")
    STRETCHED("stretched", usesWidth = true, usesHeight = true),
    ;

    public companion object {
        public fun ofKey(key: String): AspectRatio? = entries.firstOrNull { it.key == key }
    }
}
