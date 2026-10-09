/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins

import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl
import com.pandulapeter.kubriko.shared.SceneEditorConnection

fun createAnnoyedPenguinsGameStateHolder(
    webRootPathName: String,
    isSceneEditorEnabled: Boolean,
    sceneEditorConnection: SceneEditorConnection?,
    isLoggingEnabled: Boolean,
): AnnoyedPenguinsGameStateHolder = AnnoyedPenguinsGameStateHolderImpl(
    webRootPathName = webRootPathName,
    isSceneEditorEnabled = isSceneEditorEnabled,
    sceneEditorConnection = sceneEditorConnection,
    isLoggingEnabled = isLoggingEnabled,
    isForSceneEditor = false,
)