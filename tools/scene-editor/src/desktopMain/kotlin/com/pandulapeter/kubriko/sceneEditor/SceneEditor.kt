/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sceneEditor.implementation.InternalSceneEditor
import com.pandulapeter.kubriko.serialization.SerializationManager

/**
 * The real implementation of [SceneEditorContract]: a Desktop-only visual editor for [Editable] actors.
 *
 * Depend on `tool-scene-editor-noop` instead to strip it from release builds.
 */
object SceneEditor : SceneEditorContract {

    override fun show(
        defaultSceneFilename: String?,
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        title: String,
    ) = application {
        SceneEditor(
            defaultSceneFilename = defaultSceneFilename,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = serializationManager,
            customManagers = customManagers,
            onCloseRequest = ::exitApplication,
            title = title,
        )
    }

    @Composable
    override operator fun invoke(
        defaultSceneFilename: String?,
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        sceneEditorMode: SceneEditorMode,
        title: String,
        onCloseRequest: () -> Unit,
    ) = InternalSceneEditor(
        defaultSceneFilename = defaultSceneFilename,
        defaultSceneFolderPath = defaultSceneFolderPath,
        serializationManager = serializationManager,
        customManagers = customManagers,
        sceneEditorMode = sceneEditorMode,
        title = title,
        onCloseRequest = onCloseRequest,
    )
}