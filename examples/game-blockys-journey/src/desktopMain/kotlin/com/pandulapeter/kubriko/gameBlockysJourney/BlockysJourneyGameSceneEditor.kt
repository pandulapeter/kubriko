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
import com.pandulapeter.kubriko.sceneEditor.SceneEditor
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.scene_editor_title
import org.jetbrains.compose.resources.stringResource

fun main() = BlockysJourneyGameStateHolderImpl(
    webRootPathName = "",
    isSceneEditorEnabled = true,
    sceneEditorConnection = null,
    isLoggingEnabled = false,
).let { stateHolder ->
    SceneEditor.show(
        serializationManager = stateHolder.backgroundSerializationManager,
        customManagers = stateHolder.customManagersForSceneEditor,
    )
}

@Composable
fun BlockysJourneyGameSceneEditor(
    sceneEditorConnection: SceneEditorConnection,
    defaultSceneFolderPath: String,
) {
    if (sceneEditorConnection.isVisible.collectAsState().value) {
        val stateHolder = remember {
            BlockysJourneyGameStateHolderImpl(
                webRootPathName = "",
                isSceneEditorEnabled = true,
                sceneEditorConnection = null,
                isLoggingEnabled = false,
            )
        }
        SceneEditor(
            defaultSceneFilename = LoadingManager.SCENE_NAME,
            defaultSceneFolderPath = defaultSceneFolderPath,
            serializationManager = stateHolder.backgroundSerializationManager,
            customManagers = stateHolder.customManagersForSceneEditor,
            title = stringResource(Res.string.scene_editor_title),
            onCloseRequest = sceneEditorConnection::close,
        )
    }
}