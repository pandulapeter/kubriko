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
 * The family of letters a Manager type's text is drawn in, with the Compose `FontFamily` it becomes: Compose's generic
 * families, which each platform maps to faces of its own, so they look different from one platform to the next, and the
 * web, which has no system fonts, may draw them all in its default one. Font files the game bundles are planned for a text
 * that has to look alike everywhere.
 */
@Serializable
@Description("A generic family of letters, which each platform draws in a face of its own.")
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
