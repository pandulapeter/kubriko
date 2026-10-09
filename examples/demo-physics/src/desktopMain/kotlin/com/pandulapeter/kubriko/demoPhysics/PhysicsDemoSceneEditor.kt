/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoPhysics.implementation.managers.PhysicsDemoManager
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.sceneEditor.SceneEditorMode
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.scene_editor_title
import org.jetbrains.compose.resources.stringResource

fun main() = SceneEditor.show(
    defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
    serializationManager = PhysicsDemoStateHolderImpl(
        isSceneEditorEnabled = true,
        sceneEditorConnection = null,
        isLoggingEnabled = false,
    ).serializationManager,
)

@Composable
fun PhysicsDemoSceneEditor(
    sceneEditorConnection: SceneEditorConnection,
    defaultSceneFolderPath: String,
) {
    if (sceneEditorConnection.isVisible.collectAsState().value) {
        val stateHolder = remember {
            PhysicsDemoStateHolderImpl(
                isSceneEditorEnabled = true,
                sceneEditorConnection = null,
                isLoggingEnabled = false,
            )
        }
        SceneEditor(
            defaultSceneFilename = PhysicsDemoManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = stateHolder.serializationManager,
            customManagers = emptyList(),
            title = stringResource(Res.string.scene_editor_title),
            onCloseRequest = sceneEditorConnection::close,
            sceneEditorMode = SceneEditorMode.Connected(
                sceneJson = sceneEditorConnection.sceneJson.value,
                onSceneJsonChanged = { sceneEditorConnection.sceneJson.value = it },
            ),
        )
    }
}