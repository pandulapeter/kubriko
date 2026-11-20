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

import com.pandulapeter.kubrikoCreator.ui.theme.DEFAULT_INTERFACE_SCALE
import com.pandulapeter.kubrikoCreator.ui.theme.ThemeChoice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This user's settings for Kubriko Creator on this computer, kept in the settings folder ([CreatorSettingsStore]), so that
 * they work without a project and are never shared with one.
 *
 * @param isAiEnabled Whether any AI feature exists at all: off, Creator shows no AI surface, serves no tools, starts no
 *   agent, and sends nothing anywhere.
 * @param isFollowingAi Whether the editor goes to what each of the AI's changes is about, as it makes them.
 * @param aiFollowDelayInMillis How long the editor shows one of the AI's changes at least before it goes to the next one,
 *   so that quick steps don't make it flash.
 * @param isTurnReviewShown Whether the assistant ends each turn that changed the project with Keep and Undo.
 * @param interfaceScale How much smaller or larger than Material's own sizes everything is drawn.
 * @param defaultProjectFolder Where New project creates projects unless told otherwise, the home folder when null.
 * @param sceneGridSize The side of a square of the Scene editor's grid, in scene units.
 * @param isSnappingToGrid Whether the Scene editor starts with snapping to its grid on.
 * @param panelWidths The sizes in dp the user dragged the panels to (the side panels' widths, the console's height), by
 *   their `PanelWidths.Panel.id`; the others have their default size. Its key predates the console's height.
 */
@Serializable
internal data class CreatorSettings(
    @SerialName("isAiEnabled") val isAiEnabled: Boolean = true,
    @SerialName("theme") val theme: ThemeChoice = ThemeChoice.SYSTEM,
    @SerialName("interfaceScale") val interfaceScale: Float = DEFAULT_INTERFACE_SCALE,
    @SerialName("defaultProjectFolder") val defaultProjectFolder: String? = null,
    @SerialName("panelWidths") val panelWidths: Map<String, Float> = emptyMap(),
    @SerialName("isFollowingAi") val isFollowingAi: Boolean = true,
    @SerialName("aiFollowDelayInMillis") val aiFollowDelayInMillis: Int = DEFAULT_AI_FOLLOW_DELAY,
    @SerialName("isTurnReviewShown") val isTurnReviewShown: Boolean = false,
    @SerialName("sceneGridSize") val sceneGridSize: Int = DEFAULT_SCENE_GRID_SIZE,
    @SerialName("isSnappingToGrid") val isSnappingToGrid: Boolean = true,
) {
    companion object {
        /**
         * The interface scales offered, the default among them.
         */
        val interfaceScales = listOf(0.75f, DEFAULT_INTERFACE_SCALE, 1f, 1.15f, 1.3f)

        /**
         * The delays in milliseconds offered for following the AI, the default among them.
         */
        val aiFollowDelays = listOf(0, DEFAULT_AI_FOLLOW_DELAY, 600, 1_000)

        /**
         * The Scene editor's grid sizes offered, the default among them.
         */
        val sceneGridSizes = listOf(4, 8, DEFAULT_SCENE_GRID_SIZE, 32, 64)
    }
}

private const val DEFAULT_AI_FOLLOW_DELAY = 300
private const val DEFAULT_SCENE_GRID_SIZE = 16
