/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This user's settings for Kubriko Creator on this computer, kept in the settings folder ([CreatorSettingsStore]), so that
 * they work without a project and are never shared with one.
 *
 * @param isAiEnabled Whether any AI feature exists at all: off, Creator shows no AI surface, serves no tools, starts no
 *   agent, and sends nothing anywhere.
 * @param interfaceScale How much smaller or larger than Material's own sizes everything is drawn.
 * @param defaultProjectFolder Where New project creates projects unless told otherwise, the home folder when null.
 */
@Serializable
internal data class CreatorSettings(
    @SerialName("isAiEnabled") val isAiEnabled: Boolean = true,
    @SerialName("theme") val theme: ThemeChoice = ThemeChoice.SYSTEM,
    @SerialName("interfaceScale") val interfaceScale: Float = DEFAULT_INTERFACE_SCALE,
    @SerialName("defaultProjectFolder") val defaultProjectFolder: String? = null,
) {
    companion object {
        const val DEFAULT_INTERFACE_SCALE = 0.85f

        /**
         * The interface scales offered, the default among them.
         */
        val interfaceScales = listOf(0.75f, DEFAULT_INTERFACE_SCALE, 1f, 1.15f, 1.3f)
    }
}
