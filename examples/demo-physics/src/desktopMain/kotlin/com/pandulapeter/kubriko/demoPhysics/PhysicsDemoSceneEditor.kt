/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoPhysics.implementation.isSceneEditorVisible
import com.pandulapeter.kubriko.demoPhysics.implementation.managers.PhysicsDemoManager
import com.pandulapeter.kubriko.demoPhysics.implementation.sceneJson
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode

fun main() = SceneEditor.show(
    defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
    serializationManager = PhysicsDemoStateHolderImpl(
        isSceneEditorEnabled = true,
    ).serializationManager,
)

@Composable
fun PhysicsDemoSceneEditor(
    defaultSceneFolderPath: String,
) {
    if (isSceneEditorVisible.collectAsState().value) {
        SceneEditor(
            defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = PhysicsDemoStateHolderImpl(
                isSceneEditorEnabled = true,
            ).serializationManager,
            title = "Scene Editor - Physics Demo",
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