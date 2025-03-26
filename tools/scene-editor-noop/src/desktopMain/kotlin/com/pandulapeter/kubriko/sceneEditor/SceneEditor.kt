/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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

object SceneEditor : SceneEditorContract {
    /**
     * TODO: Documentation
     */
    override fun show(
        defaultSceneFilename: String?,
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        title: String,
    ) = Unit

    /**
     * TODO: Documentation
     */
    @Composable
    override operator fun invoke(
        defaultSceneFilename: String?,
        defaultSceneFolderPath: String,
        serializationManager: SerializationManager<EditableMetadata<*>, Editable<*>>,
        customManagers: List<Manager>,
        sceneEditorMode: SceneEditorMode,
        title: String,
        onCloseRequest: () -> Unit,
    ) = Unit
}