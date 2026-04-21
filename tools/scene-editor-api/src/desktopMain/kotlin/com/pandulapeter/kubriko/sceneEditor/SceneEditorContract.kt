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
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.serialization.SerializationManager

/**
 * Contract for the Scene Editor tool.
 *
 * The Scene Editor allows for visual manipulation of [Editable] actors on Desktop.
 */
interface SceneEditorContract {

    /**
     * Shows the Scene Editor in a new window.
     *
     * @param defaultSceneFilename The name of the scene file to load by default.
     * @param defaultSceneFolderPath The path to the folder containing scene files.
     * @param serializationManager The manager used for serializing/deserializing actors.
     * @param customManagers Additional engine managers to include in the editor's engine instance.
     * @param title The title of the editor window.
     */
    fun show(
        defaultSceneFilename: String? = null,
        defaultSceneFolderPath: String = "./src/commonMain/composeResources/files/scenes",
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager> = emptyList(),
        title: String = "Scene Editor",
    )

    /**
     * Embeds the Scene Editor as a Composable.
     *
     * @param defaultSceneFilename The name of the scene file to load by default.
     * @param defaultSceneFolderPath The path to the folder containing scene files.
     * @param serializationManager The manager used for serializing/deserializing actors.
     * @param customManagers Additional engine managers to include in the editor's engine instance.
     * @param sceneEditorMode The operational mode of the editor.
     * @param title The title of the editor.
     * @param onCloseRequest Callback when the user attempts to close the editor.
     */
    @Composable
    operator fun invoke(
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        title: String,
        onCloseRequest: () -> Unit,
    ) = invoke(
        defaultSceneFilename = null,
        defaultSceneFolderPath = defaultSceneFolderPath,
        serializationManager = serializationManager,
        customManagers = customManagers,
        sceneEditorMode = SceneEditorMode.Normal,
        title = title,
        onCloseRequest = onCloseRequest,
    )

    // TODO: Remove this function once default arguments in member Composables become supported.
    // https://issuetracker.google.com/issues/165812010
    @Composable
    operator fun invoke(
        defaultSceneFilename: String?,
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        title: String,
        onCloseRequest: () -> Unit,
    ) = invoke(
        defaultSceneFilename = defaultSceneFilename,
        defaultSceneFolderPath = defaultSceneFolderPath,
        serializationManager = serializationManager,
        customManagers = customManagers,
        sceneEditorMode = SceneEditorMode.Normal,
        title = title,
        onCloseRequest = onCloseRequest,
    )

    @Composable
    operator fun invoke(
        defaultSceneFilename: String?, // TODO: = null,
        defaultSceneFolderPath: String, // TODO:  = "./src/commonMain/composeResources/files/scenes",
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>, // TODO: = emptyList(),
        sceneEditorMode: SceneEditorMode, // TODO:  = SceneEditorMode.Normal,
        title: String, // TODO:  = "Scene Editor",
        onCloseRequest: () -> Unit,
    )
}