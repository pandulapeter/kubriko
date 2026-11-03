/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widths of the side panels beside the workspace's editors.
 */
public object PanelWidths {

    /**
     * A side panel the user can resize, with its [default] width, the [side] of its splitter it is on, and the [id] its
     * width is kept under in Creator's settings, which must never change.
     */
    public enum class Panel(
        public val default: Dp,
        public val id: String,
        public val side: PanelSide,
    ) {
        SIDEBAR(260.dp, "sidebar", PanelSide.START),
        AI(360.dp, "ai", PanelSide.END),
        VERSION_CONTROL(300.dp, "versionControl", PanelSide.END),
        SCENE_ACTORS(220.dp, "sceneActors", PanelSide.START),
        SCENE_INSPECTOR(260.dp, "sceneInspector", PanelSide.END),
    }
}
