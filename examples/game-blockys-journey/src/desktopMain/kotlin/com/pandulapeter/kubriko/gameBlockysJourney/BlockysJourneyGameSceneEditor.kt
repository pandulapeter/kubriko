/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolderImpl
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.isSceneEditorVisible
import com.pandulapeter.kubriko.sceneEditor.SceneEditor

fun main() = SceneEditor.show(
    serializationManager = BlockysJourneyGameStateHolderImpl(
        webRootPathName = "",
        isSceneEditorEnabled = true,
    ).serializationManager,
)

@Composable
fun BlockysJourneyGameSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = BlockysJourneyGameStateHolderImpl(
                webRootPathName = "",
                isSceneEditorEnabled = true,
            ).serializationManager,
            title = "Scene Editor - Blocky's Journey",
            onCloseRequest = { isSceneEditorVisible.value = false },
        )
    }
}