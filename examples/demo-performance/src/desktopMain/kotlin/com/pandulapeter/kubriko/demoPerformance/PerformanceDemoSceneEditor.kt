/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolderImpl
import com.pandulapeter.kubriko.demoPerformance.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoPerformance.implementation.managers.PerformanceDemoManager
import com.pandulapeter.kubriko.demoPerformance.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode

fun main() = SceneEditor.show(
    defaultSceneFilename = PerformanceDemoManager.SCENE_NAME,
    serializationManager = PerformanceDemoStateHolderImpl().serializationManager,
)

@Composable
fun PerformanceDemoSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultSceneFilename = PerformanceDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = PerformanceDemoStateHolderImpl().serializationManager,
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