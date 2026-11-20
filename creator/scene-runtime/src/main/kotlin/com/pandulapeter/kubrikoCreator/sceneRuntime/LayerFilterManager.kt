/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * The Scene editor's own Manager, in the Kubriko instance that shows a Scene only: it leaves out the drawing of the
 * layers [filter] hides, through the one way Kubriko lets a Manager change a layer, so neither the engine nor the Actors
 * change for it.
 */
internal class LayerFilterManager(
    private val filter: StateFlow<SceneLayerFilter>,
) : Manager() {
    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>): Modifier {
        val isHidden = layerIndex != null && layerIndex in filter.collectAsState().value.hidden
        return if (isHidden) modifier.drawWithContent { } else modifier
    }
}
