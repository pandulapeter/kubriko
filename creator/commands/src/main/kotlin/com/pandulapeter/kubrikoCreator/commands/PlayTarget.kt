/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A place the game can be played: a window on this computer, and later a browser or a device.
 *
 * @param id How the AI and the saved choice name it, the same each time this computer finds it.
 * @param name What the place calls itself (a device's model), or null for one that is named by its [kind] alone.
 */
@Serializable
public data class PlayTarget(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: Kind,
    @SerialName("name") val name: String? = null,
) {
    /**
     * What kind of place a [PlayTarget] is.
     */
    @Serializable
    public enum class Kind {
        /**
         * A window of its own on this computer, which hot reload and the game tools reach.
         */
        @SerialName("desktop")
        DESKTOP,
    }

    public companion object {
        /**
         * A window of its own on this computer, where the game can always be played.
         */
        public val DESKTOP: PlayTarget = PlayTarget(id = "desktop", kind = Kind.DESKTOP)
    }
}
