/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolderImpl
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.isSceneEditorVisible
import com.pandulapeter.kubriko.sceneEditor.SceneEditor

fun main() = BlockysJourneyGameStateHolderImpl(
    webRootPathName = "",
    isSceneEditorEnabled = true,
    isLoggingEnabled = false,
).let { stateHolder ->
    SceneEditor.show(
        serializationManager = stateHolder.backgroundSerializationManager,
        customManagers = stateHolder.customManagersForSceneEditor,
    )
}

@Composable
fun BlockysJourneyGameSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        val stateHolder = remember {
            BlockysJourneyGameStateHolderImpl(
                webRootPathName = "",
                isSceneEditorEnabled = true,
                isLoggingEnabled = false,
            )
        }
        SceneEditor(
            defaultSceneFilename = LoadingManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = stateHolder.backgroundSerializationManager,
            customManagers = stateHolder.customManagersForSceneEditor,
            title = "Scene Editor - Blocky's Journey",
            onCloseRequest = { isSceneEditorVisible.value = false },
        )
    }
}