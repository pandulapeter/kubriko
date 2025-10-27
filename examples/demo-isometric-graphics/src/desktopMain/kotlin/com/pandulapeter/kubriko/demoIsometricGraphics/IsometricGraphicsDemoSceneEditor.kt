/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.managers.IsometricGraphicsDemoManager
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode

fun main() = SceneEditor.show(
    defaultSceneFilename = IsometricGraphicsDemoManager.SCENE_NAME,
    serializationManager = IsometricGraphicsDemoStateHolderImpl(
        isSceneEditorEnabled = true,
        isLoggingEnabled = false,
    ).serializationManager,
)

@Composable
fun IsometricGraphicsDemoSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        val stateHolder = remember {
            IsometricGraphicsDemoStateHolderImpl(
                isSceneEditorEnabled = true,
                isLoggingEnabled = false,
            )
        }
        SceneEditor(
            defaultSceneFilename = IsometricGraphicsDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = stateHolder.serializationManager,
            customManagers = emptyList(),
            title = "Scene Editor - Performance Demo",
            onCloseRequest = { isSceneEditorVisible.value = false },
            sceneEditorMode = sceneJson?.let { sceneJson ->
                SceneEditorMode.Connected(
                    sceneJson = sceneJson.value,
                    onSceneJsonChanged = { sceneJson.value = it },
                )
            } ?: SceneEditorMode.Normal,
        )
    }
}