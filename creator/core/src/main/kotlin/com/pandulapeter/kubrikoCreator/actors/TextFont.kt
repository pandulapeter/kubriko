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

import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The family of letters a Manager type's text is drawn in, with the Compose `FontFamily` it becomes. These are the ones
 * every platform has, so a text looks alike everywhere it is played.
 */
@Serializable
@Description("A family of letters every platform has.")
public enum class TextFont(
    internal val fontFamily: String,
) {
    @SerialName("default")
    DEFAULT("Default"),

    @SerialName("sansSerif")
    SANS_SERIF("SansSerif"),

    @SerialName("serif")
    SERIF("Serif"),

    @SerialName("monospace")
    MONOSPACE("Monospace"),

    @SerialName("cursive")
    CURSIVE("Cursive"),
}
