/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.isSceneEditorVisible
import com.pandulapeter.kubriko.sceneEditor.SceneEditor

fun main() = AnnoyedPenguinsGameStateHolderImpl(
    webRootPathName = "",
    isSceneEditorEnabled = true,
    isLoggingEnabled = false,
    isForSceneEditor = true,
).let { stateHolder ->
    SceneEditor.show(
        serializationManager = stateHolder.serializationManager,
        customManagers = stateHolder.customManagersForSceneEditor,
    )
}

@Composable
fun AnnoyedPenguinsGameSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        val stateHolder = remember {
            AnnoyedPenguinsGameStateHolderImpl(
                webRootPathName = "",
                isSceneEditorEnabled = true,
                isLoggingEnabled = false,
                isForSceneEditor = true,
            )
        }
        SceneEditor(
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = stateHolder.serializationManager,
            customManagers = stateHolder.customManagersForSceneEditor,
            title = "Scene Editor - Annoyed Penguins",
            onCloseRequest = { isSceneEditorVisible.value = false },
        )
    }
}